package ru.urfu.javapools.poolslibrary.pool.wrappers;

import java.util.concurrent.ConcurrentMap;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.PoolItem;

public abstract class PWBaseWrapper<TK,TV> implements IPool<TK,TV> {

	protected final IPool<TK,TV> _basePool;	
	
	public PWBaseWrapper(IPool<TK,TV> basePool) {		
		_basePool = basePool;
	}

	@Override
	public TV obtain(TK key, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws NoAvailableObjectException, ObjectsMaxCountReachedException,	ObjectCreationFailedException,
			   InvalidPoolOperationException, PoolObjectObtainingInterruptedException {
		
		return _basePool.obtain(key, createDelegateIfNoObjectIsAvailable);
	}

	@Override
	public void release(TK key, TV objectToRelease)
		throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException {
		
		_basePool.release(key, objectToRelease);
	}
	
	@Override
	public void close() throws Exception {
		_basePool.close();	
	}

	@Override
	public ConcurrentMap<TK, PoolItem<TK, TV>> getKeyToPoolItem() {
		return _basePool.getKeyToPoolItem();
	}
}
