package ru.urfu.javapools.poolslibrary.pool.basicfunctionality;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.objectutilization.GoneObjectEvent;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizationListener;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.PoolItem;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.PoolItemSettings;

public class PoolItemsStorage<TK,TV> implements IPool<TK,TV>,
												IObjectUtilizationListener<TK,TV> {
	
	private final PoolItemsStorageSettings _settings;
	private final ConcurrentHashMap<TK,PoolItem<TK,TV>> _keyToPoolItem;
	
	private final IPoolObjectActions<TV> _objectActions;
    private final IObjectUtilizer<TK, TV> _objectUtilizer;
	
	public PoolItemsStorage (PoolItemsStorageSettings settings,
            				 IPoolObjectActions<TV> objectActions,
            				 IObjectUtilizer<TK, TV> objectUtilizer) {
		_settings = settings;
		_objectActions = objectActions;
		_objectUtilizer = objectUtilizer;
		
		_keyToPoolItem = new ConcurrentHashMap<TK, PoolItem<TK,TV>>();
		_objectUtilizer.addListener(this);
	}
	
	@Override
	public TV obtain(TK key, FunctionThatMayThrow<TK, TV> createDelegateIfNoObjectIsAvailable)
		throws NoAvailableObjectException, ObjectsMaxCountReachedException,
			   ObjectCreationFailedException, PoolObjectObtainingInterruptedException {
			
		PoolItem<TK,TV> poolItem = _keyToPoolItem.computeIfAbsent(key, this::createPoolItem);
		return poolItem.obtain(createDelegateIfNoObjectIsAvailable);
	}

	@Override
	public void release(TK key, TV objectToRelease)	throws InvalidPoolOperationException {
	}
	
	@Override
	public void onObjectUtilization(GoneObjectEvent<TK,TV> goneObjectEvent) {
		
		PoolItem<TK,TV> poolItem = _keyToPoolItem.get(goneObjectEvent.getKey());
		if (poolItem != null)
			poolItem.markObjectForKilling(goneObjectEvent.getPoolObject());
	}	
	
	@Override
	public void close()	throws Exception {
		
		_objectUtilizer.removeListener(this);
		
		for (PoolItem<TK,TV> poolItem : _keyToPoolItem.values())
			poolItem.close();
	
		//_keyToPoolItem.values().parallelStream().forEach(poolItem -> poolItem.close());
	}	

	@Override
	public ConcurrentMap<TK, PoolItem<TK, TV>> getKeyToPoolItem() {
		return _keyToPoolItem;
	}
	
    private PoolItem<TK, TV> createPoolItem(TK key) {
    	
    	PoolItemSettings<TK> poolItemSettings = new PoolItemSettings<TK>() {{
    		setKey(key);
    		setMarkObtainedObjectAsNotAvailable(_settings.getAllowOnlyOneUserPerObject());
    		setMaxObjectsCount(_settings.getMaxObjectsCountPerKey());
        }};
        
        Collection<TV> availableObjectsStorage = _settings.getBalancingStrategy().createStorage();

        return new PoolItem<TK, TV>(poolItemSettings, availableObjectsStorage, _objectActions);
    }
}
