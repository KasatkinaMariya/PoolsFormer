package ru.urfu.javapools.poolslibrary.pool.basicfunctionality;

public class PoolItemsStorageSettings {
	
	private LoadBalancingStrategy _balancingStrategy;
	private boolean _allowOnlyOneUserPerObject;
	private int _maxObjectsCountPerKey;
	
	public LoadBalancingStrategy getBalancingStrategy() {
		return _balancingStrategy;
	}
	
	public boolean getAllowOnlyOneUserPerObject() {
		return _allowOnlyOneUserPerObject;
	}

	public int getMaxObjectsCountPerKey() {
		return _maxObjectsCountPerKey;
	}

	public PoolItemsStorageSettings setBalancingStrategy(LoadBalancingStrategy balancingStrategy) {
		_balancingStrategy = balancingStrategy;
		return this;
	}

	public PoolItemsStorageSettings setAllowOnlyOneUserPerObject(boolean allowOnlyOneUserPerObject) {
		_allowOnlyOneUserPerObject = allowOnlyOneUserPerObject;
		return this;
	}

	public PoolItemsStorageSettings setMaxObjectsCountPerKey(int maxObjectsCountPerKey) {
		_maxObjectsCountPerKey = maxObjectsCountPerKey;
		return this;
	}	
}