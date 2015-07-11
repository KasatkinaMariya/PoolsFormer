package ru.urfu.javapools.poolslibrary.mocks;

import static org.mockito.Mockito.*;

import org.mockito.stubbing.Answer;

import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class CreateDelegateMocks {

	private static Integer _st_numberToStartFrom;
	
	public static FunctionThatMayThrow<TestKey, TestResource> getNewSerial(int numberToStartFrom)
		throws Exception
	{		
		@SuppressWarnings("unchecked")
		FunctionThatMayThrow<TestKey, TestResource> delegate = mock(FunctionThatMayThrow.class, new UnstubbedMethodAnswer());

		_st_numberToStartFrom = numberToStartFrom - 1;
		Answer<TestResource> createResourceAnswer =
				invocation -> new TestResource((++_st_numberToStartFrom).toString());
		doAnswer(createResourceAnswer).when(delegate).apply(any(TestKey.class));

		return delegate;
	}
	
	public static FunctionThatMayThrow<TestKey, TestResource> getNewSerialWithKey()
		throws Exception
	{
		@SuppressWarnings("unchecked")
		FunctionThatMayThrow<TestKey, TestResource> delegate = mock(FunctionThatMayThrow.class);

		Answer<TestResource> createResourceAnswer = invocation -> {
			System.out.println("delegate invoked");
			TestKey key = invocation.getArgumentAt(0, TestKey.class);
			String resourceValue = String.format("%s %s", key.getIdentifier(), ++_st_numberToStartFrom);
			return new TestResource(resourceValue);
		};
		doAnswer(createResourceAnswer).when(delegate).apply(any(TestKey.class));

		return delegate;
	}
	
	public static FunctionThatMayThrow<TestKey, TestResource> getNewThrowing(Throwable throwable)
		throws Exception
	{
		@SuppressWarnings("unchecked")
		FunctionThatMayThrow<TestKey, TestResource> delegate = mock(FunctionThatMayThrow.class, new UnstubbedMethodAnswer());

		doThrow(throwable).when(delegate).apply(any(TestKey.class));

		return delegate;
	}
}
