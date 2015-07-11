package ru.urfu.javapools.poolslibrary.controller;

@SuppressWarnings("serial")
public class PoolException extends Exception {

	private Object _key;
	
	public PoolException (Object key, String message) {
		this(key, message, null);
	}
	
	public PoolException (Object key, String message, Throwable cause) {
		super(message, cause);
		_key = key;
	}
	
	public Object getKey() {
		return _key;
	}
}