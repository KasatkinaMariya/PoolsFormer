package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

import ru.urfu.javapools.poolslibrary.controller.PoolException;

@SuppressWarnings("serial")
public class ObjectsMaxCountReachedException extends PoolException {

	private int _maxObjectCount;
	
	private static final String _MESSAGE_PATTERN = "Object with key='%s' wasn't created because" +
			  									   " max objects count %s is already reached";
	
	public ObjectsMaxCountReachedException (Object key, int maxObjectCount) {
		super(key,String.format(_MESSAGE_PATTERN, key, maxObjectCount));
		_maxObjectCount = maxObjectCount;
	}
	
	public int getMaxObjectCount() {
		return _maxObjectCount;
	}
}