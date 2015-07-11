package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

public class PoolItemSettings<TK> {
	
	private TK _key;
	private int _maxObjectsCount;
	private boolean _markObtainedObjectAsNotAvailable;
	
	public TK getKey() {
		return _key;
	}

	public boolean getMarkObtainedObjectAsNotAvailable() {
		return _markObtainedObjectAsNotAvailable;
	}
	
	public int getMaxObjectsCount() {
		return _maxObjectsCount;
	}

	public PoolItemSettings<TK> setKey(TK key) {
		_key = key;
		return this;
	}

	public PoolItemSettings<TK> setMaxObjectsCount(int maxObjectsCount) {
		_maxObjectsCount = maxObjectsCount;
		return this;
	}

	public PoolItemSettings<TK> setMarkObtainedObjectAsNotAvailable(boolean markObtainedObjectAsNotAvailable) {
		_markObtainedObjectAsNotAvailable = markObtainedObjectAsNotAvailable;
		return this;
	}	
}
