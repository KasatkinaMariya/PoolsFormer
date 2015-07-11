package ru.urfu.javapools.poolslibrary.pool.wrappers;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;

public class PWObjectResettingWrapper<TK,TV> extends PWBaseWrapper<TK,TV> {

	private final IPoolObjectActions<TV> _objectActions;
	private final IObjectUtilizer<TK,TV> _objectUtilizer;
	
	public PWObjectResettingWrapper(IPool<TK, TV> basePool,
									IPoolObjectActions<TV> objectActions,
									IObjectUtilizer<TK,TV> objectUtilizer) {
		
		super(basePool);
		_objectActions = objectActions;
		_objectUtilizer = objectUtilizer;
	}

	@Override
	public void release(TK key, TV objectToRelease)
		throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException {
		
		if (!_objectActions.reset(objectToRelease))
			_objectUtilizer.utilize(key, objectToRelease, this);
		
		_basePool.release(key, objectToRelease);
	}
}
