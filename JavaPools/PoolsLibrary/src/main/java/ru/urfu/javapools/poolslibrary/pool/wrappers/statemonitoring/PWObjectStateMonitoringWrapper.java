package ru.urfu.javapools.poolslibrary.pool.wrappers.statemonitoring;

import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.locks.CleaningSynchronizer;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.pool.wrappers.PWBaseWrapper;

public class PWObjectStateMonitoringWrapper<TK,TV> extends PWBaseWrapper<TK,TV> {

	private final PWObjectStateMonitoringSettings _settings;
	private final WeakHashMap<TV, ObjectLifetimeData<TK>> _objectToLifetimeData;
	private final boolean _shouldWatchTimestamps;
	
    private Thread _cleaningThread;
    private final CleaningSynchronizer _cleaningSynchronizer = new CleaningSynchronizer();
    private final static int _MILLS_TO_JOIN_CLEANING_THREAD = 1000;
	
    private final IPoolObjectActions<TV> _objectActions;
    private final IObjectUtilizer<TK, TV> _objectUtilizer;

    public PWObjectStateMonitoringWrapper(PWObjectStateMonitoringSettings settings,
            							  IPool<TK, TV> basePool,
            							  IPoolObjectActions<TV> objectActions,
            							  IObjectUtilizer<TK, TV> objectUtilizer) {
    	
    	super(basePool);
    	
    	_settings = settings;
        _objectActions = objectActions;
        _objectUtilizer = objectUtilizer;
        
        _objectToLifetimeData = new WeakHashMap<TV, ObjectLifetimeData<TK>>();
        _shouldWatchTimestamps = _settings.getMaxObjectIdleTimeSpanInSeconds() != null
        						 || _settings.getMaxObjectLifetimeInSeconds() != null;
        
        _cleaningThread = new Thread(() -> {
        	while (!Thread.interrupted()) {
        		try {
					Thread.sleep(_settings.getTimeSpanBetweenRevivalsInSeconds() * 1000);
					_cleaningSynchronizer.lockCleaning();
				} catch (InterruptedException e) {
					break;
				}
        		
        		try {
        			dropLifelessObjectsAndWakeupOthers();
        		} finally {        		
        			_cleaningSynchronizer.unlockCleaning();
        		}
        	}
        });
        _cleaningThread.start();
    }
	
	@Override
	public TV obtain(TK key, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws NoAvailableObjectException, ObjectsMaxCountReachedException,	ObjectCreationFailedException,
			   InvalidPoolOperationException, PoolObjectObtainingInterruptedException {
		
		try {
			_cleaningSynchronizer.lockPoolOperation();
		} catch (InterruptedException e) {
			throw new PoolObjectObtainingInterruptedException(key, e);
		}
		
		try {
			TV obtained = _basePool.obtain(key, createDelegateIfNoObjectIsAvailable);
			putOrUpdateLifetimeData(key, obtained);
			return obtained;
		} finally {
			_cleaningSynchronizer.unlockPoolOperation();
		}
	}
	
	@Override
	public void release(TK key, TV objectToRelease)
		throws InvalidPoolOperationException, PoolObjectReleasingInterruptedException {
	
		try {
			_cleaningSynchronizer.lockPoolOperation();
		} catch (InterruptedException e) {
			throw new PoolObjectReleasingInterruptedException(key, objectToRelease, e);
		}
		
		if (_shouldWatchTimestamps)		
			putOrUpdateLifetimeData(key, objectToRelease);
		
		try {
			super.release(key, objectToRelease);
		} finally {
			_cleaningSynchronizer.unlockPoolOperation();
		}
	}
	
	@Override
	public void close() throws Exception {
		
		_cleaningThread.interrupt();
		_cleaningThread.join(_MILLS_TO_JOIN_CLEANING_THREAD);
		super.close();
	}
	
	public Map<TV, ObjectLifetimeData<TK>> getObjectToLifetimeData () {
		return _objectToLifetimeData;
	}
	
	public void dropLifelessObjectsAndWakeupOthers() {
		
		for (Entry<TV,ObjectLifetimeData<TK>> objectToData : _objectToLifetimeData.entrySet()) {
			
			TV poolObject = objectToData.getKey();
			if (poolObject == null)
				continue;
			
			boolean tooOld = !dateIsCloseToNow(objectToData.getValue().getCreationTimeStamp(),
											   _settings.getMaxObjectLifetimeInSeconds());
			boolean notActive = !dateIsCloseToNow(objectToData.getValue().getLastUsageTimeStamp(),
												  _settings.getMaxObjectIdleTimeSpanInSeconds());
			boolean shouldBeUtilized = tooOld || notActive || !_objectActions.ping(poolObject);
			
			if (shouldBeUtilized) {
				TK key = objectToData.getValue().getKey();				
				_objectUtilizer.utilize(key, poolObject, this);			
			}
		}
	}
		
	private synchronized void putOrUpdateLifetimeData(TK key, TV poolobject) {
		
		ObjectLifetimeData<TK> previousLifetimeData = _objectToLifetimeData.get(poolobject);
		if (previousLifetimeData != null)
			previousLifetimeData.update();
		else
			_objectToLifetimeData.put(poolobject, new ObjectLifetimeData<TK>(key));
	}
	
	private boolean dateIsCloseToNow (long dateToTest, Integer maximumDifferenceInSeconds) {		
		
		return (maximumDifferenceInSeconds != null)
			   ? System.currentTimeMillis() - dateToTest < maximumDifferenceInSeconds * 1000
			   : true;
	}
}
