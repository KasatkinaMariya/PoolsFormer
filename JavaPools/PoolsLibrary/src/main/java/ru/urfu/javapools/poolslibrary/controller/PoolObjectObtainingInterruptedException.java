package ru.urfu.javapools.poolslibrary.controller;

@SuppressWarnings("serial")
public class PoolObjectObtainingInterruptedException extends PoolException {

	private static final String _DEFAULT_MESSAGE_PATTERN = "User thread was interrupted during obtaining object with key='%s'";
	
	public PoolObjectObtainingInterruptedException (Object key, Throwable cause) {
		this(key,String.format(_DEFAULT_MESSAGE_PATTERN, key), cause);
	}
	
	public PoolObjectObtainingInterruptedException (Object key, String message, Throwable cause) {
		super(key, message, cause);
	}
}