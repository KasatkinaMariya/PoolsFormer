package ru.urfu.javapools.poolslibrary.mocks;

import static org.mockito.Mockito.*;
import org.mockito.stubbing.Answer;

import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class ObjectActionsMocks {

	public static String SUBSTRING_OF_INVALID_OBJECT = "invalid";
	
	private static Answer<Boolean> _isValidAnswer = invocation -> {
		TestResource givenResource = invocation.getArgumentAt(0, TestResource.class);
		return !givenResource.getValue().contains(SUBSTRING_OF_INVALID_OBJECT);  		
	};

    public static IPoolObjectActions<TestResource> getNewSuccessful() {

		@SuppressWarnings("unchecked")
		IPoolObjectActions<TestResource> objectActionsMock = mock(IPoolObjectActions.class, new UnstubbedMethodAnswer());
    				
    	doAnswer(_isValidAnswer).when(objectActionsMock).isValid(any(TestResource.class));    	
    	doReturn(true).when(objectActionsMock).ping(any(TestResource.class));
    	doNothing().when(objectActionsMock).close(any(TestResource.class));
    	
    	return objectActionsMock;
    }
    
    public static IPoolObjectActions<TestResource> getNewFailingOnPing()
    {
		@SuppressWarnings("unchecked")
		IPoolObjectActions<TestResource> throwingActionsMock = mock(IPoolObjectActions.class, new UnstubbedMethodAnswer());
    		
    	doAnswer(_isValidAnswer).when(throwingActionsMock).isValid(any(TestResource.class));    	
    	doReturn(false).when(throwingActionsMock).ping(any(TestResource.class));

        return throwingActionsMock;
    }
}
