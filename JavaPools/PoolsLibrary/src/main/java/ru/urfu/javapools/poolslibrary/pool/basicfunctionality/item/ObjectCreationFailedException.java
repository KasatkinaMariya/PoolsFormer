package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

import ru.urfu.javapools.poolslibrary.controller.PoolException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;

@SuppressWarnings({"serial", "rawtypes"})
public class ObjectCreationFailedException extends PoolException {
	
	private FunctionThatMayThrow _usedCreateDelegate;
	
	private static final String _MESSAGE_PATTERN = "Creation of object with key='%s' failed. Look at cause for details";
	
	public ObjectCreationFailedException (Object key, FunctionThatMayThrow usedCreateDelegate, Throwable cause) {
				
		super(key, String.format(_MESSAGE_PATTERN, key), cause);		
		_usedCreateDelegate = usedCreateDelegate;		
	}
	
	public FunctionThatMayThrow getUsedCreateDelegate() {
		return _usedCreateDelegate;
	}
}
