package ru.urfu.javapools.poolslibrary.mocks;

import java.io.FileNotFoundException;

import static org.mockito.Mockito.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectObtainingInterruptedException;
import ru.urfu.javapools.poolslibrary.controller.PoolObjectReleasingInterruptedException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

@SuppressWarnings("unchecked")
public class PoolMocks {
	
    public static final String RESOURCE_VALUE_AFTER_WAITINGS = "success";
    public static final Throwable CAUSE_OF_CREATION_FAILURE = new FileNotFoundException(); 
    public static final TestResource SAME_OBTAINED_RESOURCE = new TestResource("gfd");	
	
    private static final Answer<TestResource> _NO_OBJECT_ANSWER = invocation -> {
    	TestKey key = invocation.getArgumentAt(0, TestKey.class);
    	throw new NoAvailableObjectException(key);
    };
    private static final Answer<TestResource> _MAX_COUNT_REACHED_ANSWER = invocation -> {
    	TestKey key = invocation.getArgumentAt(0, TestKey.class);
    	throw new ObjectsMaxCountReachedException(key, 100);
    };
    
	public static IPool<TestKey, TestResource> getNewReturningSerialWithKey()
    	throws InvalidPoolOperationException, NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, PoolObjectObtainingInterruptedException, PoolObjectReleasingInterruptedException {
       
		IPool<TestKey,TestResource> successPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());

        doAnswer(new SerialWithKeyAnswer()).when(successPoolMock).obtain(any(TestKey.class), any(FunctionThatMayThrow.class));        
        doNothing().when(successPoolMock).release(any(TestKey.class), any(TestResource.class));
        
        return successPoolMock;
    }
   
	public static IPool<TestKey, TestResource> getNewReturningSameObject()
    	throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, InvalidPoolOperationException, PoolObjectObtainingInterruptedException, PoolObjectReleasingInterruptedException {    
    	
		IPool<TestKey,TestResource> sameObjectPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());
		
        doReturn(SAME_OBTAINED_RESOURCE).when(sameObjectPoolMock).obtain(any(TestKey.class), any(FunctionThatMayThrow.class));
        doNothing().when(sameObjectPoolMock).release(any(TestKey.class), any(TestResource.class));
        
        return sameObjectPoolMock;
    }
    
    public static IPool<TestKey, TestResource> getNewThrowingNoObjectException()
    	throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, InvalidPoolOperationException, PoolObjectObtainingInterruptedException {
    	
		IPool<TestKey,TestResource> noObjectPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());
        doAnswer(_NO_OBJECT_ANSWER).when(noObjectPoolMock).obtain(any(TestKey.class), eq(null));        
        return noObjectPoolMock;
    }    
    
	public static IPool<TestKey, TestResource> getNewThrowingNoObjectExceptionThreeTimes()
        throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, InvalidPoolOperationException, PoolObjectObtainingInterruptedException {
    
        IPool<TestKey,TestResource> noObjectThreeTimesPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());
        
        doAnswer(_NO_OBJECT_ANSWER)
        .doAnswer(_NO_OBJECT_ANSWER)
        .doAnswer(_NO_OBJECT_ANSWER)
        .doReturn(new TestResource(RESOURCE_VALUE_AFTER_WAITINGS))
        .when(noObjectThreeTimesPoolMock).obtain(any(TestKey.class), any(FunctionThatMayThrow.class));

        return noObjectThreeTimesPoolMock;
    }
	
    public static IPool<TestKey, TestResource> getNewThrowingMaxCountReachedException()
        throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, InvalidPoolOperationException, PoolObjectObtainingInterruptedException {
        	
    	IPool<TestKey,TestResource> noObjectPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());
    	doAnswer(_MAX_COUNT_REACHED_ANSWER).when(noObjectPoolMock).obtain(any(TestKey.class), eq(null));        
        return noObjectPoolMock;
    }

    public static IPool<TestKey, TestResource> getNewThrowing()
    	throws NoAvailableObjectException, ObjectsMaxCountReachedException, ObjectCreationFailedException, InvalidPoolOperationException, PoolObjectObtainingInterruptedException, PoolObjectReleasingInterruptedException {
    	
        IPool<TestKey, TestResource> throwingPoolMock = mock(IPool.class, new UnstubbedMethodAnswer());
        
        Answer<TestResource> objectCreationFailedAnswer = invocation -> {
        	TestKey key = invocation.getArgumentAt(0, TestKey.class);
        	FunctionThatMayThrow<TestKey,TestResource> createDelegate = invocation.getArgumentAt(1, FunctionThatMayThrow.class);
        	Throwable cause = CAUSE_OF_CREATION_FAILURE;
        	throw new ObjectCreationFailedException(key, createDelegate, cause);
        };
        doAnswer(objectCreationFailedAnswer).when(throwingPoolMock).obtain(any(TestKey.class), any(FunctionThatMayThrow.class));

        Answer<TestResource> invalidOperationAnswer = invocation -> {
        	TestKey key = invocation.getArgumentAt(0, TestKey.class);
        	TestResource object = invocation.getArgumentAt(1, TestResource.class);
        	throw new InvalidPoolOperationException(key, object, "error on releasing");
        };
        doAnswer(invalidOperationAnswer).when(throwingPoolMock).release(any(TestKey.class), any(TestResource.class));
        
        return throwingPoolMock;
    }
}

class SerialWithKeyAnswer implements Answer<TestResource> {

	private int _counter;
	
	@Override
	public TestResource answer(InvocationOnMock invocation) throws Throwable {
    	TestKey key = invocation.getArgumentAt(0, TestKey.class);
    	String resourceValue = String.format("%s %s", key.getIdentifier(), ++_counter);
    	return new TestResource(resourceValue);
	}	
}
