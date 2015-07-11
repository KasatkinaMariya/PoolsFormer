package ru.urfu.javapools.poolslibrary.mocks;

import static org.mockito.Mockito.*;

import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizationListener;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class ObjectUtilizerMocks {
	
    @SuppressWarnings("unchecked")
	public static IObjectUtilizer<TestKey, TestResource> getNew() {
    	
    	IObjectUtilizer<TestKey, TestResource> objectUtilizerMock = mock(IObjectUtilizer.class, new UnstubbedMethodAnswer());
    	
    	doNothing().when(objectUtilizerMock).addListener(any(IObjectUtilizationListener.class));
    	doNothing().when(objectUtilizerMock).removeListener(any(IObjectUtilizationListener.class));
    	doNothing().when(objectUtilizerMock).utilize(any(TestKey.class), any(TestResource.class), any());

        return objectUtilizerMock;
    }
}