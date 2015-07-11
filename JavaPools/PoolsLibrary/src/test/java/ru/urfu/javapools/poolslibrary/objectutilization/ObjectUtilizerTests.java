package ru.urfu.javapools.poolslibrary.objectutilization;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

@RunWith(MockitoJUnitRunner.class)
public class ObjectUtilizerTests {

	private IObjectUtilizer<TestKey, TestResource> _utilizer;
	
	@Mock private IObjectUtilizationListener<TestKey, TestResource> _listenerMock;
	@Mock private IObjectUtilizationListener<TestKey, TestResource> _anotherListenerMock;
	
	private final TestKey _testKey = new TestKey(743);
	private final TestResource _testResource = new TestResource("toUtilize");
	
	@Before
	public void before () {
		_utilizer = new ObjectUtilizer<TestKey, TestResource>();
	}
	
	@Test
	public void listenerAddedItselfToListeners_utilizerRemembersIt () {
		
		_utilizer.addListener(_listenerMock);
		
		assertThat(_utilizer.getListenersCount(), is(1));
	}
	
	@Test
	public void listenerAddedItselfToListenersTwice_utilizerRemembersItOnlyOnce () {
		
		_utilizer.addListener(_listenerMock);
		_utilizer.addListener(_listenerMock);
		
		assertThat(_utilizer.getListenersCount(), is(1));
	}
	
	@Test
	public void differentListenersAddedThemselvesToListeners_utilizersRemembersEach () {
		
		_utilizer.addListener(_listenerMock);
		_utilizer.addListener(_anotherListenerMock);
		
		assertThat(_utilizer.getListenersCount(), is(2));
	}
	
	@Test
	public void utilizationWasCalled_utilizerCallsListeners () {
		
		_utilizer.addListener(_listenerMock);
		_utilizer.addListener(_anotherListenerMock);
		
		_utilizer.utilize(_testKey, _testResource, _listenerMock);
		
		GoneObjectEvent<TestKey,TestResource> expectedEvent = new GoneObjectEvent<TestKey,TestResource>() {{
			setReporter(_listenerMock);
			setKey(_testKey);
			setPoolObject(_testResource);
		}};
		verify(_listenerMock).onObjectUtilization(expectedEvent);
		verify(_anotherListenerMock).onObjectUtilization(expectedEvent);
	}
	
	@Test
	public void noListeners_utilizerDoesNotThrow () {
		_utilizer.utilize(_testKey, _testResource, _listenerMock);
	}
	
	@Test
	public void nullListenerWasAdded_utilizerIgnoresItSilently () {
		
		_utilizer.addListener(null);
		
		assertThat(_utilizer.getListenersCount(), is(0));
	}
	
	@Test
	public void listenerWasRemoved_utilizerForgetsThatListener () {
		
		_utilizer.addListener(_listenerMock);
		_utilizer.addListener(_anotherListenerMock);
		
		_utilizer.removeListener(_listenerMock);
		
		assertThat(_utilizer.getListenersCount(), is(1));
	}
	
	@Test
	public void removingOfUnknownListener_utilizerIgnoresItSilently () {
		
		_utilizer.addListener(_listenerMock);
		
		_utilizer.removeListener(_anotherListenerMock);
		
		assertThat(_utilizer.getListenersCount(), is(1));
	}
	
	@Test
	public void removingOfNullListener_utilizerIgnoresItSilently () {
		
		_utilizer.addListener(_listenerMock);
		
		_utilizer.removeListener(null);
		
		assertThat(_utilizer.getListenersCount(), is(1));
	}
}