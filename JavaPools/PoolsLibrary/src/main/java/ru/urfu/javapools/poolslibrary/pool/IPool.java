package ru.urfu.javapools.poolslibrary.pool;

import java.util.concurrent.ConcurrentMap;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.PoolItem;

public interface IPool<TK,TV> extends AutoCloseable {
	
	 TV obtain(TK key, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException,
			   InvalidPoolOperationException, PoolObjectObtainingInterruptedException;
	 
     void release(TK key, TV objectToRelease)
    	throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException;
     
     ConcurrentMap<TK, PoolItem<TK, TV>> getKeyToPoolItem();
}