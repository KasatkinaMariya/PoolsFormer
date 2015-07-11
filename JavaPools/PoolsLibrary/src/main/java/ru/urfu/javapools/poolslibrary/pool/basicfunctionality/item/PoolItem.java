package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.lang.MyReference;
import ru.urfu.javapools.poolslibrary.locks.FairLockWithPrecedence;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;

public class PoolItem<TK, TV> implements AutoCloseable {

	private final PoolItemSettings<TK> _settings;
	private final Collection<TV> _availableObjects;
	private final HashSet<TV> _notAvailableObjects = new HashSet<TV>();
	private final HashSet<TV> _objectsToKill = new HashSet<TV>();

	private final FairLockWithPrecedence _obtainReleaseLock = new FairLockWithPrecedence();
	
	private final IPoolObjectActions<TV> _objectActions;

	private static final String _MESSAGE_MARKING_IS_OFF = "Operation of marking object as available is invalid because marking was ordered to be off";
	private static final String _MESSAGE_UNMARKING_DECLINED = "Marking object as available has been declined";
	private static final String _MESSAGE_REASON_CURRENTLY_AVAILABLE = "because it's currently available. Object should be marked as not available first";
	private static final String _MESSAGE_REASON_STRANGER = "because this object wasn't created by pool, it's a stranger";

	public PoolItem(PoolItemSettings<TK> settings,
					Collection<TV> availableObjectsStorage,
					IPoolObjectActions<TV> objectActions) {
		
		_settings = settings;
		_availableObjects = availableObjectsStorage;
		_objectActions = objectActions;
	}
	
	public TV obtain(FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws NoAvailableObjectException, ObjectsMaxCountReachedException,
			   ObjectCreationFailedException, PoolObjectObtainingInterruptedException {
		
		try {
			_obtainReleaseLock.lock();
		} catch (InterruptedException e) {
			throw new PoolObjectObtainingInterruptedException(_settings.getKey(), e);
		}
	
		try {
			if (_notAvailableObjects.size() == _settings.getMaxObjectsCount())
				throw new ObjectsMaxCountReachedException(_settings.getKey(), _settings.getMaxObjectsCount());

			MyReference<TV> out = new MyReference<TV>(null);
			if (tryGetExistingAvailableObject(out) || tryCreateNewObject(out,createDelegateIfNoObjectIsAvailable)) {			
				markBeforeGiving(out.get());
				return out.get();
			}

			throw new NoAvailableObjectException(_settings.getKey());
		} finally {
			_obtainReleaseLock.unlock();
		}
	}

	public void release(TV objectToUnmark) throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException {

		if (!_settings.getMarkObtainedObjectAsNotAvailable())
			throw new InvalidPoolOperationException(_settings.getKey(), objectToUnmark, _MESSAGE_MARKING_IS_OFF);

		try {
			_obtainReleaseLock.lockImportant();
		} catch (InterruptedException e) {
			throw new PoolObjectReleasingInterruptedException(_settings.getKey(), objectToUnmark, e);
		}
		
		try {
			if (!_notAvailableObjects.remove(objectToUnmark)) {
				String invalidUnmarkOperationMessage = _MESSAGE_UNMARKING_DECLINED + " " + (_availableObjects.contains(objectToUnmark)
																							? _MESSAGE_REASON_CURRENTLY_AVAILABLE
																							: _MESSAGE_REASON_STRANGER);
				throw new InvalidPoolOperationException(_settings.getKey(), objectToUnmark, invalidUnmarkOperationMessage);
			}

			if (!closeIfBad(objectToUnmark))
				_availableObjects.add(objectToUnmark);
		} finally {
			_obtainReleaseLock.unlock();
		}
	}

	public void markObjectForKilling(TV toKill) {

		if (_availableObjects.remove(toKill))
			return;
		
		if (_notAvailableObjects.contains(toKill))
			_objectsToKill.add(toKill);
	}

	@Override
	public void close() throws Exception {
		
		Iterator<TV> avaliableObjectsIterator = _availableObjects.iterator();
		while (avaliableObjectsIterator.hasNext())
			_objectActions.close(avaliableObjectsIterator.next());

		for (TV notAvailableObject : _notAvailableObjects)
			_objectActions.close(notAvailableObject);
	}

	public int getAllObjectsCount() {
		return _availableObjects.size() + _notAvailableObjects.size();
	}

	private boolean tryGetExistingAvailableObject(MyReference<TV> out) {

		Iterator<TV> avaliableObjectIterator = _availableObjects.iterator();
		while (avaliableObjectIterator.hasNext()) {
			out.set(avaliableObjectIterator.next());
			avaliableObjectIterator.remove();
			if (!closeIfBad(out.get()))
				return true;
		}

		return false;
	}

	private boolean tryCreateNewObject(MyReference<TV> out, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws ObjectCreationFailedException {
		
		if (createDelegateIfNoObjectIsAvailable == null)
			return false;

		try {
			out.set(createDelegateIfNoObjectIsAvailable.apply(_settings.getKey()));
			return true;
		} catch (Exception e) {
			throw new ObjectCreationFailedException(_settings.getKey(), createDelegateIfNoObjectIsAvailable, e);
		}
	}

	private boolean closeIfBad(TV object) {

		if (!_objectsToKill.remove(object) && _objectActions.isValid(object))
			return false;

		_objectActions.close(object);
		return true;
	}

	private void markBeforeGiving(TV toProvide) {

		if (_settings.getMarkObtainedObjectAsNotAvailable())
			_notAvailableObjects.add(toProvide);
		else
			_availableObjects.add(toProvide);
	}
}
