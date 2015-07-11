package ru.urfu.javapools.poolslibrary.controller;

@SuppressWarnings("serial")
public class PoolObjectReleasingInterruptedException extends PoolException {

	private Object _object;
	private static final String _MESSAGE_PATTERN = "User thread was interrupted during releasing object='%s' with key='%s'";	
	
	public PoolObjectReleasingInterruptedException (Object key, Object object, Throwable cause) {
		super(key,String.format(_MESSAGE_PATTERN, object, key), cause);
		_object = object;
	}
	
	public Object getObject() {
		return _object;
	}
}