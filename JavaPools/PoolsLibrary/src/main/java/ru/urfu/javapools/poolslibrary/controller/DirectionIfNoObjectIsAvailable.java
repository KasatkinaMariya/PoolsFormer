package ru.urfu.javapools.poolslibrary.controller;

import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;

public class DirectionIfNoObjectIsAvailable<TK,TV> {
	
	private int _attemptsNumber;
	private int _oneIntervalBetweenAttemptsInSeconds;
	private FunctionThatMayThrow<TK,TV> _createMethod;
	
	public int getAttemptsNumber() {
		return _attemptsNumber;
	}
	
	public int getOneIntervalBetweenAttemptsInSeconds() {
		return _oneIntervalBetweenAttemptsInSeconds;
	}
	
	public FunctionThatMayThrow<TK, TV> getCreateMethod() {
		return _createMethod;
	}
	
	public DirectionIfNoObjectIsAvailable<TK,TV> setAttemptsNumber(int attemptsNumber) {
		_attemptsNumber = attemptsNumber;
		return this;
	}
	
	public DirectionIfNoObjectIsAvailable<TK,TV> setOneIntervalBetweenAttemptsInSeconds(int oneIntervalBetweenAttemptsInSeconds) {
		_oneIntervalBetweenAttemptsInSeconds = oneIntervalBetweenAttemptsInSeconds;
		return this;
	}
	
	public DirectionIfNoObjectIsAvailable<TK,TV> setCreateMethod(FunctionThatMayThrow<TK, TV> createMethod) {
		_createMethod = createMethod;
		return this;
	}	
}