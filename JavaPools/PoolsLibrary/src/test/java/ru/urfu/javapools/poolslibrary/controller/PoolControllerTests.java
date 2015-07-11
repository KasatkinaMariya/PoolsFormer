package ru.urfu.javapools.poolslibrary.controller;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import static ru.urfu.javapools.poolslibrary.utils.DateUtils.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.internal.matchers.GreaterThan;
import org.mockito.internal.matchers.LessThan;

import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.matchers.CreationFailedExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.InvalidOperationExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.PoolExceptionMatcher;
import ru.urfu.javapools.poolslibrary.mocks.PoolMocks;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectsMaxCountReachedException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class PoolControllerTests {

	private PoolController<TestKey, TestResource> _controller;

    private final TestKey _key = new TestKey(8);
    private final TestKey _key1 = new TestKey(100);
    private final TestKey _key2 = new TestKey(200);
    private FunctionThatMayThrow<TestKey,TestResource> _createDelegate;
    private final DirectionIfNoObjectIsAvailable<TestKey, TestResource> _noObjectDirection
    										= new DirectionIfNoObjectIsAvailable<TestKey, TestResource>();;

    private IPool<TestKey, TestResource> _successfulPoolMock;
    private IPool<TestKey, TestResource> _successfulSameObjectPoolMock;
    private IPool<TestKey, TestResource> _throwingNoObjectPoolMock;
    private IPool<TestKey, TestResource> _throwingNoObjectThreeTimesPoolMock;
    private IPool<TestKey, TestResource> _throwingMaxCountReachedPoolMock;
    private IPool<TestKey, TestResource> _throwingPoolMock;
	
	@Rule
	public ExpectedException _thrown = ExpectedException.none();	
  
    @Before
    public void before() throws Exception {
    	
        _successfulPoolMock = PoolMocks.getNewReturningSerialWithKey();
        _successfulSameObjectPoolMock = PoolMocks.getNewReturningSameObject();
        _throwingNoObjectPoolMock = PoolMocks.getNewThrowingNoObjectException();
        _throwingNoObjectThreeTimesPoolMock = PoolMocks.getNewThrowingNoObjectExceptionThreeTimes();
        _throwingMaxCountReachedPoolMock = PoolMocks.getNewThrowingMaxCountReachedException();
        _throwingPoolMock = PoolMocks.getNewThrowing();
    }
    
    private void initWith(IPool<TestKey, TestResource> poolMock) {
    	_controller = new PoolController<TestKey, TestResource>(poolMock);
    }
    
    @Test
    public void obtainWasCalled_poolIsCalled_providedObjectIsReturned() throws Exception {
    	
    	initWith(_successfulPoolMock);

        TestResource obtained = _controller.obtain(_key, null);

        assertThat(obtained.getValue(), is(_key.getIdentifier() + " 1"));
        verify(_successfulPoolMock).obtain(_key, null);
    }
    
    @Test
    public void numberOfAttemptsIsSpecified_poolIsRecalledAndFinalResultIsGood() throws Exception {
    
    	initWith(_throwingNoObjectThreeTimesPoolMock);
        _noObjectDirection.setAttemptsNumber(5);
        
        TestResource obtained = _controller.obtain(_key, _noObjectDirection);

        assertThat(obtained.getValue(), is(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS));
        verify(_throwingNoObjectThreeTimesPoolMock, times(4)).obtain(_key, null);
    }
    
    
	@Test
    public void controllerPerformedSpecifiedNumberOfAttempts_controllerStopsReattemptingAndThrowsNoObjectException() throws Exception {
    
    	initWith(_throwingNoObjectPoolMock);
        _noObjectDirection.setAttemptsNumber(7);
       
        _thrown.expect(NoAvailableObjectException.class);
        _thrown.expect(new PoolExceptionMatcher(_key));        
        
        _controller.obtain(_key, _noObjectDirection);        
        verify(_throwingNoObjectPoolMock, times(7)).obtain(_key, _noObjectDirection.getCreateMethod());
    }
	
	@Test
    public void poolThrewMaxCountReachedExceptionOnLastAttempt_controllerRethrowsItAsIs() throws Exception {
    
    	initWith(_throwingMaxCountReachedPoolMock);
        _noObjectDirection.setAttemptsNumber(7);
       
        _thrown.expect(ObjectsMaxCountReachedException.class);
        _thrown.expect(new PoolExceptionMatcher(_key));        
        
        _controller.obtain(_key, _noObjectDirection);        
        verify(_throwingMaxCountReachedPoolMock, times(7)).obtain(_key, _noObjectDirection.getCreateMethod());
    }
	
    @Test
    public void nullNoObjectDirection_controllerPerformsOneAttemptWithoutCreateDelegate() throws Exception {
    	
    	initWith(_successfulPoolMock);

        _controller.obtain(_key, null);

        verify(_successfulPoolMock).obtain(_key, null);
    }
    
    @Test
    public void createDelegateIsSpecified_poolIsCalledWithIt_butOnlyLastTime() throws Exception {

    	_createDelegate = key -> new TestResource(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS);
        _noObjectDirection.setAttemptsNumber(4);
        _noObjectDirection.setCreateMethod(_createDelegate);
        initWith(_throwingNoObjectThreeTimesPoolMock);
        
        TestResource obtained = _controller.obtain(_key, _noObjectDirection);

        assertThat(obtained.getValue(), is(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS));        
        InOrder inOrder = inOrder(_throwingNoObjectThreeTimesPoolMock);
        inOrder.verify(_throwingNoObjectThreeTimesPoolMock, times(3)).obtain(_key, null);
        inOrder.verify(_throwingNoObjectThreeTimesPoolMock).obtain(_key, _createDelegate);
        inOrder.verify(_throwingNoObjectThreeTimesPoolMock, never()).obtain(any(), any());
    }

    @Test
    public void reattempsIntervalIsSpecified_controllerWaitsBeforeReattempt() throws Exception {
    	
    	initWith(_throwingNoObjectThreeTimesPoolMock);
        _noObjectDirection.setAttemptsNumber(8);
        _noObjectDirection.setOneIntervalBetweenAttemptsInSeconds(1);
        
        long startTimestamp = currentTime();
        TestResource obtained = _controller.obtain(_key, _noObjectDirection);
        long executionTimeInMills = currentTime() - startTimestamp;

        assertThat(executionTimeInMills, allOf(new GreaterThan<Long>(2900L), new LessThan<Long>(4000L)));
        assertThat(obtained.getValue(), is(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS));
    }
    
    @Test
    public void reattempsIntervalIsEqualToZero_controllerDoesNotWait() throws Exception {
    
    	initWith(_throwingNoObjectThreeTimesPoolMock);
        _noObjectDirection.setAttemptsNumber(8);
        _noObjectDirection.setOneIntervalBetweenAttemptsInSeconds(0);

        long startTimestamp = currentTime();
        TestResource obtained = _controller.obtain(_key, _noObjectDirection);
        long executionTimeInMills = currentTime() - startTimestamp;

        assertThat(executionTimeInMills, new LessThan<Long>(100L));
        assertThat(obtained.getValue(), is(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS));
    }
    
    @Test
    public void poolThrew_controllerStopsReattemptingAndThrowsPoolException() throws Exception {
    	
    	initWith(_throwingPoolMock);
        _noObjectDirection.setAttemptsNumber(3);

        _thrown.expect(PoolException.class);
        _thrown.expect(new PoolExceptionMatcher(_key));
        _thrown.expectCause(new CreationFailedExceptionMatcher(_key, null, PoolMocks.CAUSE_OF_CREATION_FAILURE));
        _thrown.expectMessage(String.format("Something failed during attempt #1 of obtaining object with key='%s'. " +
                							"Look at cause for details", _key));
        
        _controller.obtain(_key, _noObjectDirection);
        
        verify(_throwingPoolMock).obtain(_key, null);
    }
    
    @Test
    public void controllerCanRecallKeyForEachObtainedValue() throws Exception {
    
    	initWith(_successfulPoolMock);

        TestResource obtained11 = _controller.obtain(_key1, null);
        TestResource obtained12 = _controller.obtain(_key1, null);
        TestResource obtained2 = _controller.obtain(_key2, null);

        assertThat(_controller.getKeyByObject(obtained11), is(_key1));
        assertThat(_controller.getKeyByObject(obtained12), is(_key1));
        assertThat(_controller.getKeyByObject(obtained2), is(_key2));
    }
    
    @Test
    public void releaseWasCalled_poolIsCalled() throws Exception {
    
    	initWith(_successfulPoolMock);
        TestResource obtained11 = _controller.obtain(_key1, null);
        TestResource obtained12 = _controller.obtain(_key1, null);
        TestResource obtained2 = _controller.obtain(_key2, null);
        
        _controller.release(obtained11);
        _controller.release(obtained12);
        _controller.release(obtained2);

        verify(_successfulPoolMock).release(_key1, obtained11);
        verify(_successfulPoolMock).release(_key1, obtained12);
        verify(_successfulPoolMock).release(_key2, obtained2);
    }
    
    @Test
    public void anotherObtainingAndReleasingOfSameObject_controllerDoesNotThrowAndRemembersKey() throws Exception {
    	
    	initWith(_successfulSameObjectPoolMock);
    	
    	TestResource obtained1 = _controller.obtain(_key, null);
    	_controller.release(obtained1);
    	TestResource obtained2 = _controller.obtain(_key, null);
    	_controller.release(obtained2);
    	
    	assertThat(_controller.getKeyByObject(obtained1), is(_key));
    	assertThat(obtained1, allOf(is(obtained2), is(PoolMocks.SAME_OBTAINED_RESOURCE)));
    }
    
    @Test
    public void releasingOfNotObtainedObject_IvalidOperationExceptionIsThrown() throws Exception {
    	
    	initWith(_successfulPoolMock);
        TestResource stranger = new TestResource("");
    	
    	_thrown.expect(InvalidPoolOperationException.class);
    	_thrown.expectMessage("Only obtained objects are allowed to be released");
    	_thrown.expect(new InvalidOperationExceptionMatcher(null, stranger));
    	
    	_controller.release(stranger);
    }
    
    @Test
    public void waitingWasInterrupted_controllerThrowsNoObjectExceptionAndSetsInterruptedStatus() throws Exception {
    	
    	initWith(_throwingNoObjectThreeTimesPoolMock);
    	_noObjectDirection.setAttemptsNumber(5);
    	_noObjectDirection.setOneIntervalBetweenAttemptsInSeconds(1);
    	Runnable fakeUserRunnable = () -> {
    		try {
				_controller.obtain(_key, _noObjectDirection);
			} catch (PoolObjectReleasingInterruptedException e) {
				assertThat(e.getKey(), is(_key));
				assertThat(Thread.currentThread().isInterrupted(), is(true));
			} catch (PoolException e) {
				fail("Waiting user thread should throw PoolObjectReleasingInterruptedException, not " + e);
			}
    	};
    	Thread waitingUserThread = new Thread(fakeUserRunnable);   	

    	waitingUserThread.start();    	
    	Thread.sleep(1500);    	
    	waitingUserThread.interrupt();
    }
    
    @Test
    public void waitingWasInterrupted_controllerContinuesServingQueriesFromOtherThreads() throws Exception {
    	
    	initWith(_throwingNoObjectThreeTimesPoolMock);
    	_noObjectDirection.setAttemptsNumber(5);
    	_noObjectDirection.setOneIntervalBetweenAttemptsInSeconds(1);
    	Runnable fakeUserRunnable = () -> {
    		try {
				_controller.obtain(_key1, _noObjectDirection);
			} catch (Exception e) {}
    	};
    	Thread waitingUserThread = new Thread(fakeUserRunnable);
    	waitingUserThread.start();    	
    	Thread.sleep(1500);    	
    	waitingUserThread.interrupt();
    	
    	_noObjectDirection.setOneIntervalBetweenAttemptsInSeconds(0);
    	TestResource obtainedByAnotherThread = _controller.obtain(_key2, _noObjectDirection);
    	
    	assertThat(obtainedByAnotherThread.getValue(), is(PoolMocks.RESOURCE_VALUE_AFTER_WAITINGS));
    }
}
