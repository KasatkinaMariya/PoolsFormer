package ru.urfu.javapools.poolslibrary.controller;

import java.util.WeakHashMap;

import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.locks.NotReentrantReadWriteLock;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;

public class PoolController<TK,TV> implements AutoCloseable {
	
	private final IPool<TK,TV> _pool;
	
	private final WeakHashMap<TV,TK> _obtainedObjectToItsKey = new WeakHashMap<TV,TK>();	
	private NotReentrantReadWriteLock _obtainedObjectToKeyLock = new NotReentrantReadWriteLock();		

	private final static String _MESSAGE_PATTERN_OBTAINING_FAILED = "Something failed during attempt #%s of obtaining" +
																	" object with key='%s'. Look at cause for details";
	private final static String _MESSAGE_PATTERN_OBTAINING_INTERRUPTED = "Controller obtained object with key='%s'," +
																		 " but user thread was interrupted during finishing obtaining" +
																		 " - controller has released object";
	private final static String _MESSAGE_RELEASING_OF_STRANGER = "Only obtained objects are allowed to be released";
	
	public PoolController(IPool<TK,TV> pool) {
		_pool = pool;
	}
	
	public TV obtain (TK key, DirectionIfNoObjectIsAvailable<TK, TV> noObjectDirection)
		throws PoolException {
		
		TV toReturn = null;
		
		if (noObjectDirection == null)
			noObjectDirection = new DirectionIfNoObjectIsAvailable<TK,TV>().setAttemptsNumber(1); 
		
		int curAttemptNumber = 0;
		while (curAttemptNumber++ < noObjectDirection.getAttemptsNumber()) {	
		
			if (curAttemptNumber > 1)
				waitSafely(noObjectDirection.getOneIntervalBetweenAttemptsInSeconds(), key);			
			
			boolean isLastAttempt = curAttemptNumber == noObjectDirection.getAttemptsNumber();
			FunctionThatMayThrow<TK,TV> curDelegate = isLastAttempt ? noObjectDirection.getCreateMethod() : null;
			
			try {				
				toReturn = _pool.obtain(key, curDelegate);
				break;
			} catch (NoAvailableObjectException | ObjectsMaxCountReachedException e) {
				if (isLastAttempt)
					throw e;
			} catch (PoolException e) {
				String message = String.format(_MESSAGE_PATTERN_OBTAINING_FAILED, curAttemptNumber, key);
				throw new PoolException(key, message, e);
			}
		}

		rememberObtainedObject(key, toReturn);
		return toReturn;
	}
	
	public void release(TV objectToRelease) throws PoolException {
		
		TK key = null;
		
		try {
			key = getKeyByObject(objectToRelease);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PoolObjectReleasingInterruptedException(key, objectToRelease, e);
		}
		
		if (key == null)
			throw new InvalidPoolOperationException(null, objectToRelease, _MESSAGE_RELEASING_OF_STRANGER);
			
		_pool.release(key, objectToRelease);
	}
	
	@Override
	public void close() throws Exception {
		_pool.close();
	}
	
	public TK getKeyByObject(TV obtainedObject) throws InterruptedException {

		_obtainedObjectToKeyLock.lockRead();
		
		try {			
			return _obtainedObjectToItsKey.get(obtainedObject);
		} finally {
			_obtainedObjectToKeyLock.unlockRead();
		}
	}
	
	private void waitSafely(int secondsNumberToWait, TK requestedKey) throws PoolObjectObtainingInterruptedException {
		
		try {
			Thread.sleep(secondsNumberToWait * 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PoolObjectObtainingInterruptedException(requestedKey, e);
		}
	}
	
	private void rememberObtainedObject(TK key, TV obtainedObject) throws PoolObjectObtainingInterruptedException {
		
		try {
			if (getKeyByObject(obtainedObject) != null)
				return;
			
			_obtainedObjectToKeyLock.lockWrite();			
			try {				
				_obtainedObjectToItsKey.put(obtainedObject, key);
			} finally {
				_obtainedObjectToKeyLock.unlockWrite();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			
			Throwable cause = e;
			String message = String.format(_MESSAGE_PATTERN_OBTAINING_INTERRUPTED, key);
			
			try {
				_pool.release(key, obtainedObject);
			} catch (InvalidPoolOperationException | PoolObjectReleasingInterruptedException ee) {
				cause = ee;
				message += ". Releasing also hasn't finished. Look at cause for details";
			}
			
			throw new PoolObjectObtainingInterruptedException(key, message, cause);
		}
	}
}
