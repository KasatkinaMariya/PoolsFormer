package ru.urfu.javapools.poolslibrary.objectactions;

import ru.urfu.javapools.poolslibrary.function.ConsumerThatMayThrow;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;

public class ExplicitlyDefinedObjectActions<TV> {
	
    private FunctionThatMayThrow<TV,Boolean> _isValidDelegate;
    private ConsumerThatMayThrow<TV> _pingDelegate;
    private ConsumerThatMayThrow<TV> _resetDelegate;
    private ConsumerThatMayThrow<TV> _closeDelegate;
    
	public FunctionThatMayThrow<TV, Boolean> getIsValidDelegate() {
		return _isValidDelegate;
	}
	
	public ConsumerThatMayThrow<TV> getPingDelegate() {
		return _pingDelegate;
	}
	
	public ConsumerThatMayThrow<TV> getResetDelegate() {
		return _resetDelegate;
	}
	
	public ConsumerThatMayThrow<TV> getCloseDelegate() {
		return _closeDelegate;
	}
	
	public ExplicitlyDefinedObjectActions<TV> setIsValidDelegate(FunctionThatMayThrow<TV, Boolean> isValidDelegate) {
		_isValidDelegate = isValidDelegate;
		return this;
	}
	
	public ExplicitlyDefinedObjectActions<TV> setPingDelegate(ConsumerThatMayThrow<TV> pingDelegate) {
		_pingDelegate = pingDelegate;
		return this;
	}
	
	public ExplicitlyDefinedObjectActions<TV> setResetDelegate(ConsumerThatMayThrow<TV> resetDelegate) {
		_resetDelegate = resetDelegate;
		return this;
	}
	
	public ExplicitlyDefinedObjectActions<TV> setCloseDelegate(ConsumerThatMayThrow<TV> closeDelegate) {
		_closeDelegate = closeDelegate;
		return this;
	}
}
