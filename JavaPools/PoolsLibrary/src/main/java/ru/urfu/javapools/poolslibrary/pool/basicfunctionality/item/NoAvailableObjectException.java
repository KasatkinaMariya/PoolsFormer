package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

import ru.urfu.javapools.poolslibrary.controller.PoolException;

@SuppressWarnings("serial")
public class NoAvailableObjectException extends PoolException {

	private static final String _DEFAULT_MESSAGE_PATTERN = "No available object with key='%s'";
	
	public NoAvailableObjectException (Object key) {
		super(key,String.format(_DEFAULT_MESSAGE_PATTERN, key));
	}
	
	public NoAvailableObjectException (Object key, String message, Throwable cause) {
		super(key,message, cause);
	}
}