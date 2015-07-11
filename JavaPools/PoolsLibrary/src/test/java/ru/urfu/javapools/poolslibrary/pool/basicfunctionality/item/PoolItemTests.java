package ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.matchers.CreationFailedExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.InvalidOperationExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.MaxCountReachedExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.PoolExceptionMatcher;
import ru.urfu.javapools.poolslibrary.mocks.CreateDelegateMocks;
import ru.urfu.javapools.poolslibrary.mocks.ObjectActionsMocks;
import ru.urfu.javapools.poolslibrary.mocks.PoolMocks;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class PoolItemTests {

	private PoolItem<TestKey, TestResource> _poolItem;

	private final PoolItemSettings<TestKey> _settings;
	private final TestKey _key;

	private Collection<TestResource> _availableObjectsStorageMock;
	private IPoolObjectActions<TestResource> _objectActionsMock;
	
	private FunctionThatMayThrow<TestKey,TestResource> _createDelegateMock;	

	@Rule
	public ExpectedException _thrown = ExpectedException.none();

	public PoolItemTests() {

		_key = new TestKey(846853);

		_settings = new PoolItemSettings<TestKey>() {{
			setKey(_key);
			setMarkObtainedObjectAsNotAvailable(true);
			setMaxObjectsCount(100);
		}};
	}

	@Before @SuppressWarnings("unchecked")	
	public void before() {

		_createDelegateMock = null;
		_availableObjectsStorageMock = spy(LinkedList.class);
		_objectActionsMock = ObjectActionsMocks.getNewSuccessful();

		_poolItem = new PoolItem<TestKey, TestResource>(_settings, _availableObjectsStorageMock, _objectActionsMock);
	}

	@Test
	public void availableObjectExisted_obtainReturnsIt() throws Exception {

		addAvailableObject("1");

		TestResource obtained = _poolItem.obtain(null);

		assertThat(obtained.getValue(), is("1"));
	}

	@Test
	public void neitherCreateDelegateNorAvailableObject_obtainThrowsNoAvailableObjectException() throws Exception {

		_thrown.expect(NoAvailableObjectException.class);
		_thrown.expectMessage(getExpectedNoAvailableObjectMessage());
		_thrown.expect(new PoolExceptionMatcher(_key));
		
		_poolItem.obtain(null);
	}

    @Test
    public void markingIsOnAndObjectWasObtained_obtainThrowsNoAvailableObjectException() throws Exception {  	
        
    	_settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        _poolItem.obtain(null);
        
		_thrown.expect(NoAvailableObjectException.class);
		_thrown.expectMessage(getExpectedNoAvailableObjectMessage());
		_thrown.expect(new PoolExceptionMatcher(_key));
		
        _poolItem.obtain(null);
    }

    @Test
    public void markingIsOffAndObjectWasObtained_obtainReturnsIt() throws Exception {
    	
        _settings.setMarkObtainedObjectAsNotAvailable(false);
        addAvailableObject("1");
        TestResource obtainedFirst = _poolItem.obtain(null);

        TestResource obtainedSecond = _poolItem.obtain(null);

        assertThat(obtainedFirst.getValue(), is("1"));
        assertThat(obtainedSecond, is(obtainedFirst));
    }
    
    @Test
    public void markingIsOnAndObjectWasObtainedAndUnmarked_obtainReturnsIt() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        TestResource obtainedFirst = _poolItem.obtain(null);
        _poolItem.release(obtainedFirst);

        TestResource obtainedSecond = _poolItem.obtain(null);

        assertThat(obtainedFirst.getValue(), is("1"));
        assertThat(obtainedSecond, is(obtainedFirst));
    }
    
    @Test
    public void markingIsOnAndOneAvailableObjectWasObtained_obtainReturnsAnotherAvailableObject() throws Exception {
    
    	_settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        addAvailableObject("2");
        TestResource obtainedFirst = _poolItem.obtain(null);

        TestResource obtainedSecond = _poolItem.obtain(null);
        
        assertThat(obtainedSecond, notNullValue());
        assertThat(obtainedSecond, not(obtainedFirst));
    }
    
    @Test
    public void noAvailableObjectAndMaxCountWasReached_obtainThrowsMaxCountReachedException() throws Exception {
    
        _settings.setMaxObjectsCount(2);
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        addAvailableObject("2");
        _poolItem.obtain(null);
        _poolItem.obtain(null);

		_thrown.expect(ObjectsMaxCountReachedException.class);		
		String expectedMessage = String.format("Object with key='%s' wasn't created because" +
                							   " max objects count %s is already reached",
                							   _key, 2);		
		_thrown.expectMessage(expectedMessage);
		_thrown.expect(new MaxCountReachedExceptionMatcher(_key, 2));        
        
        _poolItem.obtain(key -> new TestResource("won't be used"));
    }
    
    @Test
    public void maxCountWasReachedButSomeObjectIsAvailable_obtainReturnsIt() throws Exception {
    
        _settings.setMaxObjectsCount(2);
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        TestResource available = addAvailableObject("2");
        _poolItem.obtain(null);

        TestResource obtained = _poolItem.obtain(key -> new TestResource("won't be used"));
        
        assertThat(obtained, is(available));
    }
    
    @Test
    public void poolItemIsEmpty_obtainWithCreateDelegateReturnsNewlyCreatedObject() throws Exception {
    
    	_createDelegateMock = CreateDelegateMocks.getNewSerial(3);
    	
    	TestResource obtained = _poolItem.obtain(_createDelegateMock);

        assertThat(obtained.getValue(), is("3"));
        verify(_createDelegateMock).apply(_key);
    }
    
    @Test
    public void allObjectsAreNotAvailable_obtainWithCreateDelegateReturnsNewlyCreatedObject() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        _poolItem.obtain(null);
        _createDelegateMock = CreateDelegateMocks.getNewSerial(3);

        TestResource obtained = _poolItem.obtain(_createDelegateMock);

        assertThat(obtained.getValue(), is("3"));
        verify(_createDelegateMock).apply(_key);
    }
    
    @Test
    public void markingIsOff_referenceToCreatedObjectIsPreserved() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(false);

        _poolItem.obtain(CreateDelegateMocks.getNewSerial(1));

        assertThat(_poolItem.getAllObjectsCount(), is(1));
    }
    
    @Test
    public void markingIsOn_referenceToCreatedObjectIsPreserved() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(true);

        _poolItem.obtain(CreateDelegateMocks.getNewSerial(1));

        assertThat(_poolItem.getAllObjectsCount(), is(1));
    }
    
    @Test
    public void markingIsOffAndUnmarkingOfStrangeObject_invalidOperationExceptionIsThrown() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(false);
        TestResource unknownObject = new TestResource("asd");
        
		_thrown.expect(InvalidPoolOperationException.class);
		_thrown.expectMessage("Operation of marking object as available is invalid " +
                			  "because marking was ordered to be off");
		_thrown.expect(new InvalidOperationExceptionMatcher(_key, unknownObject));

		_poolItem.release(unknownObject);
    }
    
    @Test
    public void markingIsOffAndUnmarkingOfAvailableObject_invalidPoolOperationExceptionIsThrown() throws Exception {
    
    	_settings.setMarkObtainedObjectAsNotAvailable(false);
    	TestResource availableObject = addAvailableObject("4");

		_thrown.expect(InvalidPoolOperationException.class);
		_thrown.expectMessage("Operation of marking object as available is invalid " +
                			  "because marking was ordered to be off");
		_thrown.expect(new InvalidOperationExceptionMatcher(_key, availableObject));
     
        _poolItem.release(availableObject);
    }
    
    @Test
    public void markingIsOnAndUnmarkingOfStrangeObject_invalidOperationExceptionIsThrown() throws Exception {
    
    	_settings.setMarkObtainedObjectAsNotAvailable(true);
        TestResource unknownObject = new TestResource("asd");

		_thrown.expect(InvalidPoolOperationException.class);
		_thrown.expectMessage("Marking object as available has been declined because " +
                			  "this object wasn't created by pool, it's a stranger");
		_thrown.expect(new InvalidOperationExceptionMatcher(_key, unknownObject));    
        
        _poolItem.release(unknownObject);
    }
    
    @Test
    public void markingIsOnAndUnmarkingOfAvailableObject_invalidOperationExceptionIsThrown() throws Exception {
    
    	_settings.setMarkObtainedObjectAsNotAvailable(true);
        TestResource availableObject = addAvailableObject("2");

		_thrown.expect(InvalidPoolOperationException.class);
		_thrown.expectMessage("Marking object as available has been declined " +
                			  "because it's currently available. Object should " +
                			  "be marked as not available first");
		_thrown.expect(new InvalidOperationExceptionMatcher(_key, availableObject)); 
        
        _poolItem.release(availableObject);
    }
    
    @Test
    public void allAvailableObjectsBecameInvalid_poolItemForgotsThemAndReturnsAnotherObject() throws Exception {
    
        addAvailableObject("1" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        addAvailableObject("2" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        _createDelegateMock = CreateDelegateMocks.getNewSerial(5);

        TestResource obtained = _poolItem.obtain(_createDelegateMock);

        assertThat(obtained.getValue(), is("5"));
        verify(_createDelegateMock).apply(_key);
        assertThat(_poolItem.getAllObjectsCount(), is(1));
    }
    
    @Test
    public void availableObjectsBecameInvalid_obtainForgetsThemAndKeepsOthers() throws Exception {
    
        addAvailableObject("1" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        addAvailableObject("2" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        addAvailableObject("3");

        _poolItem.obtain(null);

        assertThat(_poolItem.getAllObjectsCount(), is(1));
    }
    
    @Test
    public void availableObjectsBecameInvalid_obtainClosesThemAndDoesNotAffectOthers() throws Exception {
    
        TestResource invalidAvailable1 = addAvailableObject("1" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        TestResource invalidAvailable2 = addAvailableObject("2" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        addAvailableObject("3");

        _poolItem.obtain(null);

        verify(_objectActionsMock).close(invalidAvailable1);
        verify(_objectActionsMock).close(invalidAvailable2);
        verify(_objectActionsMock, times(2)).close(any());
    }
    
    
    @Test
    public void markingIsOnAndUnmarkingOfObtainedInvalidObject_itIsForgottenSilently() throws Exception {
    
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        addAvailableObject("1");
        TestResource obtained = _poolItem.obtain(null);

        obtained.appendValue(ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        _poolItem.release(obtained);

        assertThat(_poolItem.getAllObjectsCount(), is(0));
    }
    
    @Test
    public void markingIsOnAndUnmarkingOfObtainedInvalidObject_objectIsClosed() throws Exception {
    
    	_settings.setMarkObtainedObjectAsNotAvailable(true);
    	TestResource invalidInFutureResource = addAvailableObject("1");
        TestResource obtained = _poolItem.obtain(null);

        obtained.appendValue(ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        _poolItem.release(obtained);
        
        verify(_objectActionsMock).close(invalidInFutureResource);
    }
    
    @Test
    public void createDelegateThrewException_creationFailedExceptionIsThrown() throws Exception {
    
        _createDelegateMock = CreateDelegateMocks.getNewThrowing(new FileNotFoundException());
              
		_thrown.expect(ObjectCreationFailedException.class);
        String expectedMessage = String.format("Creation of object with key='%s' failed. " +
                							   "Look at cause for details", _key);		
		_thrown.expectMessage(expectedMessage);
		_thrown.expectCause(org.hamcrest.CoreMatchers.isA(FileNotFoundException.class));
		_thrown.expect(new CreationFailedExceptionMatcher(_key, _createDelegateMock, PoolMocks.CAUSE_OF_CREATION_FAILURE));         
        
        _poolItem.obtain(_createDelegateMock);
    }
    
    @Test
    public void noCreateDelegateAndAllAvailableObjectsWereInvalid_obtainThrowsNoAvailableObjectException() throws Exception {
    
        addAvailableObject("1" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);
        addAvailableObject("2" + ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT);

		_thrown.expect(NoAvailableObjectException.class);
		_thrown.expectMessage(getExpectedNoAvailableObjectMessage());
		_thrown.expect(new PoolExceptionMatcher(_key));
        
        _poolItem.obtain(null);
    }
    
    @Test
    public void availableObjectsWereSaidToBeKilled_obtainThrowsNoAvailableObjectException() throws Exception {
    
        TestResource availableObject = addAvailableObject("toKill");
        _poolItem.markObjectForKilling(availableObject);
        
		_thrown.expect(NoAvailableObjectException.class);
		_thrown.expectMessage(getExpectedNoAvailableObjectMessage());
		_thrown.expect(new PoolExceptionMatcher(_key));
        
        _poolItem.obtain(null);
    }   
    
    @Test
    public void availableObjectWasSaidToBeKilled_obtainRemovesItAndClosesIt() throws Exception {
    
        TestResource availableObject = addAvailableObject("toKill");
        _poolItem.markObjectForKilling(availableObject);
        
		_thrown.expect(NoAvailableObjectException.class);
        _poolItem.obtain(null);

        assertThat(_poolItem.getAllObjectsCount(), is(0));
        verify(_objectActionsMock).close(availableObject);
    }
    
    @Test
    public void availableObjectWasSaidToBeKilled_obtainReturnsAnotherAvailableObject() throws Exception {
    
        TestResource availableObject = addAvailableObject("toKill");
        addAvailableObject("toReturn");
        _poolItem.markObjectForKilling(availableObject);
        
        TestResource obtained = _poolItem.obtain(null);

        assertThat(obtained.getValue(), is("toReturn"));
    }
    
    @Test
    public void unmarkingOfObjectSaidToBeKilled_itIsRemovedAndClosed() throws Exception {
    
        TestResource poolObject = addAvailableObject("object");
        _poolItem.obtain(null);

        _poolItem.markObjectForKilling(poolObject);
        _poolItem.release(poolObject);

        assertThat(_poolItem.getAllObjectsCount(), is(0));
        verify(_objectActionsMock).close(poolObject);
    }
    

    @Test
    public void killingOfUnknownObject_itemDoesNotThrowAndIgnoresSilently() {
    	
    	TestResource unknownObject = new TestResource("stranger");

        _poolItem.markObjectForKilling(unknownObject);
    }
    
    @Test
    public void closeWasCalled_allObjectsAreClosed() throws Exception {
    	
        _settings.setMarkObtainedObjectAsNotAvailable(true);
        TestResource object1 = addAvailableObject("1");
        TestResource object2 = addAvailableObject("2");
        _poolItem.obtain(null);        
        TestResource object3 = addAvailableObject("3");
        _poolItem.obtain(null);        
        TestResource object4 = addAvailableObject("4");

        _poolItem.close();

        verify(_objectActionsMock).close(object1);
        verify(_objectActionsMock).close(object2);
        verify(_objectActionsMock).close(object3);
        verify(_objectActionsMock).close(object4);
    }	

	private TestResource addAvailableObject(String value) {
		TestResource resource = new TestResource(value);
		_availableObjectsStorageMock.add(resource);
		return resource;
	}
	
    private String getExpectedNoAvailableObjectMessage () {
    	return String.format("No available object with key='%s'", _key);
    }
}
