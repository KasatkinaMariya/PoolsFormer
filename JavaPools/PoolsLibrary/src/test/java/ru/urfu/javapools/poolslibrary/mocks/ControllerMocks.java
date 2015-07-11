package ru.urfu.javapools.poolslibrary.mocks;

import static org.mockito.Mockito.*;
import ru.urfu.javapools.poolslibrary.controller.PoolController;
import ru.urfu.javapools.poolslibrary.controller.PoolException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

@SuppressWarnings("unchecked")
public class ControllerMocks {

	public static PoolController<TestKey,TestResource> getNewSuccessful() throws PoolException {
		
		PoolController<TestKey,TestResource> successfulControllerMock = mock(PoolController.class, new UnstubbedMethodAnswer());		
		doNothing().when(successfulControllerMock).release(any(TestResource.class));		
		return successfulControllerMock;
	}
	
	public static PoolController<TestKey,TestResource> getNewThrowing(TestKey key) throws PoolException, InterruptedException {
		
		PoolController<TestKey,TestResource> throwingControllerMock = mock(PoolController.class, new UnstubbedMethodAnswer());
		
		doThrow(new PoolException(key, "error")).when(throwingControllerMock).release(any(TestResource.class));	
		doReturn(null).when(throwingControllerMock).getKeyByObject(any(TestResource.class));
		
		return throwingControllerMock;
	}
}