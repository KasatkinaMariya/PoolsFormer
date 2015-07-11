package ru.urfu.javapools.poolslibrary.pool.wrappers;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.PoolItem;

public class PWSingleUseEnforcingWrapper<TK,TV> extends PWBaseWrapper<TK,TV> {

	public PWSingleUseEnforcingWrapper(IPool<TK, TV> basePool) {
		super(basePool);
	}
	
	@Override
	public void release(TK key, TV objectToRelease)
		throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException {
		
		PoolItem<TK,TV> poolItem = getKeyToPoolItem().get(key);
		poolItem.release(objectToRelease);
		
		_basePool.release(key, objectToRelease);
	}
}
