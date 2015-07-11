package ru.urfu.javapools.poolslibrary.controller;

@SuppressWarnings("serial")
public class InvalidPoolOperationException extends PoolException {

	private Object _object;
	
	public InvalidPoolOperationException (Object key, Object object, String message) {
		super(key,message);
		_object = object;
	}
	
	public Object getObject() {
		return _object;
	}
}
