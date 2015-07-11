package ru.urfu.javapools.poolslibrary.pool.wrappers.autoreleasing;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolController;
import ru.urfu.javapools.poolslibrary.controller.PoolException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.pool.wrappers.PWBaseWrapper;

public class PWAutoReleasingWrapper<TK, TV extends ISelfReleasingObject<TV>> extends PWBaseWrapper<TK, TV> implements ISelfReleasingObjectListener<TV> {

	private PoolController<TK, TV> _controller;
	private final IObjectUtilizer<TK, TV> _objectUtilizer;

	public PWAutoReleasingWrapper(IPool<TK, TV> basePool, IObjectUtilizer<TK, TV> objectUtilizer) {

		super(basePool);

		_objectUtilizer = objectUtilizer;
	}

	@Override
	public TV obtain(TK key, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable) throws NoAvailableObjectException,
			ObjectsMaxCountReachedException, ObjectCreationFailedException,
			InvalidPoolOperationException, PoolObjectObtainingInterruptedException {

		becomeSureControllerIsSet();

		TV obtained = _basePool.obtain(key, createDelegateIfNoObjectIsAvailable);
		obtained.setListener(this);
		return obtained;
	}

	@Override
	public void onSelfReleasing(TV selfReleasedObject) {

		try {
			_controller.release(selfReleasedObject);
		} catch (PoolException e) {
			try {
				TK key = _controller.getKeyByObject(selfReleasedObject);
				_objectUtilizer.utilize(key, selfReleasedObject, this);
			} catch (InterruptedException ee) {}
		}
	}

	public void setPoolController(PoolController<TK, TV> controller) {
		_controller = controller;
	}

	private void becomeSureControllerIsSet() throws InvalidPoolOperationException {

		if (_controller == null) {
			String noControllerMessage = "PWAutoReleasingWrapper needs specified instance of PoolController. "
					+ "Call PWAutoReleasingWrapper.setPoolController(..) before starting usage of pool";
			throw new InvalidPoolOperationException(null, null, noControllerMessage);
		}
	}
}
