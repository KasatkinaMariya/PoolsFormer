package ru.urfu.javapools.poolslibrary.mocks;

@SuppressWarnings("serial")
public class UnstubbedMethodException extends RuntimeException {

	public UnstubbedMethodException () {
		super("Unstabbed method was called");
	}
	
}
