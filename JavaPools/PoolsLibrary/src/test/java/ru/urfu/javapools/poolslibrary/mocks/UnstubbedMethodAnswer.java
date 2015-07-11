package ru.urfu.javapools.poolslibrary.mocks;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class UnstubbedMethodAnswer implements Answer<Object> {

	@Override
	public Object answer(InvocationOnMock invocation) throws Throwable {
		throw new UnstubbedMethodException();
	}	
}