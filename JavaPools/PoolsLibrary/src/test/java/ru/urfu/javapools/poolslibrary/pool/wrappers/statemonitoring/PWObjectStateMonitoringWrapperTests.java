package ru.urfu.javapools.poolslibrary.pool.wrappers.statemonitoring;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import static ru.urfu.javapools.poolslibrary.utils.DateUtils.*;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.urfu.javapools.poolslibrary.controller.InvalidPoolOperationException;
import ru.urfu.javapools.poolslibrary.function.FunctionThatMayThrow;
import ru.urfu.javapools.poolslibrary.matchers.CreationFailedExceptionMatcher;
import ru.urfu.javapools.poolslibrary.matchers.InvalidOperationExceptionMatcher;
import ru.urfu.javapools.poolslibrary.mocks.ObjectActionsMocks;
import ru.urfu.javapools.poolslibrary.mocks.ObjectUtilizerMocks;
import ru.urfu.javapools.poolslibrary.mocks.PoolMocks;
import ru.urfu.javapools.poolslibrary.objectactions.IPoolObjectActions;
import ru.urfu.javapools.poolslibrary.objectutilization.IObjectUtilizer;
import ru.urfu.javapools.poolslibrary.pool.IPool;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.NoAvailableObjectException;
import ru.urfu.javapools.poolslibrary.pool.basicfunctionality.item.ObjectCreationFailedException;
import ru.urfu.javapools.poolslibrary.testentities.TestKey;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

public class PWObjectStateMonitoringWrapperTests {

    private PWObjectStateMonitoringWrapper<TestKey, TestResource> _pool;
    
    private PWObjectStateMonitoringSettings _settings;
    private final TestKey _key;
    private FunctionThatMayThrow<TestKey, TestResource> _createDelegate;
	
    private IPool<TestKey, TestResource> _baseSuccessfulPoolMock;
    private IPool<TestKey, TestResource> _baseSameObjectPoolMock;
    private IPool<TestKey, TestResource> _baseNoObjectPoolMock;
    private IPool<TestKey, TestResource> _baseThrowingPoolMock;
    private IPoolObjectActions<TestResource> _successfulObjectActionsMock;
    private IPoolObjectActions<TestResource> _failingOnPingObjectActionsMock;
    private IObjectUtilizer<TestKey,TestResource> _objectUtilizerMock;
    
    private static final int _TIME_TOLERANCE_IN_MILLS = 50;
    
	@Rule
	public ExpectedException _thrown = ExpectedException.none();
    
    public PWObjectStateMonitoringWrapperTests() {
    	
    	setDefaultTimeTolerance(_TIME_TOLERANCE_IN_MILLS);
        _key = new TestKey(375);
    }

	@Before
	public void before() throws Exception {
		
        _settings = new PWObjectStateMonitoringSettings () {{
            setTimeSpanBetweenRevivalsInSeconds(100);
            setMaxObjectLifetimeInSeconds(3);
            setMaxObjectIdleTimeSpanInSeconds(2);
        }};

        _baseSuccessfulPoolMock = PoolMocks.getNewReturningSerialWithKey();
        _baseSameObjectPoolMock = PoolMocks.getNewReturningSameObject();
        _baseNoObjectPoolMock = PoolMocks.getNewThrowingNoObjectException();
        _baseThrowingPoolMock = PoolMocks.getNewThrowing();

        _successfulObjectActionsMock = ObjectActionsMocks.getNewSuccessful();
        _failingOnPingObjectActionsMock = ObjectActionsMocks.getNewFailingOnPing();

        _objectUtilizerMock = ObjectUtilizerMocks.getNew();
	}
    
    @Test
    public void obtainWasCalled_basePoolIsCalled() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        _createDelegate = key -> new TestResource("");

        _pool.obtain(_key, _createDelegate);

        verify(_baseSuccessfulPoolMock).obtain(_key, _createDelegate);
    }

    @Test
    public void obtainWasCalled_providedObjectIsReturned() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
  
        TestResource obtained = _pool.obtain(_key, null);

        assertThat(obtained.getValue(), is(_key.getIdentifier() + " 1"));
    }
    
    @Test
    public void releaseWasCalled_basePoolIsCalled() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource resource = addObject("asd");

        _pool.release(_key, resource);

        verify(_baseSuccessfulPoolMock).release(_key, resource);
    }

    @Test
    public void basePoolProvidesObject_objectLifetimeDataAppears() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);

        TestResource obtained = _pool.obtain(_key, null);

        ObjectLifetimeData<TestKey> lifetimeData = _pool.getObjectToLifetimeData().get(obtained);;
        assertThat(lifetimeData.getKey(), is(_key));
        assertTrue(datesAreClose(lifetimeData.getCreationTimeStamp(), currentTime()));
        assertTrue(datesAreClose(lifetimeData.getLastUsageTimeStamp(), currentTime()));
    }
    
    @Test
    public void basePoolThrewNoObjectException_poolRethrowsIt() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseNoObjectPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);

        _thrown.expect(NoAvailableObjectException.class);
        
        _pool.obtain(_key, null);
    }
    
    @Test
    public void anotherObtainingOfSameObject_creationTimestampIsNotModified() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource obtainedPreviously = _pool.obtain(_key, null);
        long previous = _pool.getObjectToLifetimeData().get(obtainedPreviously).getCreationTimeStamp();

        Thread.sleep(5);
        TestResource obtained = _pool.obtain(_key, null);

        long current = _pool.getObjectToLifetimeData().get(obtained).getCreationTimeStamp();
        assertThat(current, is(previous));
    }
    
    @Test
    public void anotherObtainingOfSameObject_lastUsageTimestampIsUpdated() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource obtainedPreviously = _pool.obtain(_key, null);
        long previous = _pool.getObjectToLifetimeData().get(obtainedPreviously).getLastUsageTimeStamp();

        Thread.sleep(10);
        TestResource obtained = _pool.obtain(_key, null);

        long current = _pool.getObjectToLifetimeData().get(obtained).getLastUsageTimeStamp();
        assertTrue(datesDifferenceIsMoreThan(previous, current, 9));
        assertTrue(datesAreClose(previous, current));
    }
    
    @Test
    public void anotherObtainingOfSameObject_rememberedObjectKeyIsPreserved() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource obtainedPreviously = _pool.obtain(_key, null);
        TestKey previous = _pool.getObjectToLifetimeData().get(obtainedPreviously).getKey();

        TestResource obtained = _pool.obtain(_key, null);

        TestKey current = _pool.getObjectToLifetimeData().get(obtained).getKey();
        assertThat(previous, is(current));
    }
    
    @Test
    public void releaseWasCalled_lastUsageTimestampIsUpdated() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource resource = addObject("asd");
        long previous = _pool.getObjectToLifetimeData().get(resource).getLastUsageTimeStamp();

        Thread.sleep(10);
        _pool.release(_key, resource);

        long current = _pool.getObjectToLifetimeData().get(resource).getLastUsageTimeStamp();
        assertTrue(datesDifferenceIsMoreThan(previous, current, 9));
        assertTrue(datesAreClose(previous, current));
    }
    
    @Test
    public void releaseWasCalled_creationTimestampIsNotModified() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource resource = addObject("asd");
        long previous = _pool.getObjectToLifetimeData().get(resource).getCreationTimeStamp();

        Thread.sleep(5);
        _pool.release(_key, resource);

        long current = _pool.getObjectToLifetimeData().get(resource).getCreationTimeStamp();
        assertThat(current, is(previous));
    }
    
    @Test
    public void releaseWasCalled_rememberedKeyIsPreserved() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource resource = addObject("asd");
        TestKey previous = _pool.getObjectToLifetimeData().get(resource).getKey();

        _pool.release(_key, resource);

        TestKey current = _pool.getObjectToLifetimeData().get(resource).getKey();
        assertThat(current, is(previous));
    }
    
    @Test
    public void releasingOfUnknownObject_notThrowsAndAddsTimestampData() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource unknownResource = new TestResource("asd");

        _pool.release(_key, unknownResource);

        assertThat(_pool.getObjectToLifetimeData().size(), is(1));
        ObjectLifetimeData<TestKey> lifetimeData = _pool.getObjectToLifetimeData().get(unknownResource);
        assertTrue(datesAreClose(lifetimeData.getCreationTimeStamp(), currentTime()));
        assertTrue(datesAreClose(lifetimeData.getLastUsageTimeStamp(), currentTime()));
    }
    
    @Test
    public void releasingOfUnknownObject_basePoolIsCalled() throws Exception {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource unknownResource = new TestResource("asd");

        _pool.release(_key, unknownResource);

        verify(_baseSuccessfulPoolMock).release(_key, unknownResource);
    }
    
    @Test
    public void onlyRevivalTimespanIsSpecified_timestampsAreNotRememberedOnRelease() throws Exception {
    
        _settings.setMaxObjectIdleTimeSpanInSeconds(null);
        _settings.setMaxObjectLifetimeInSeconds(null);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource unknownResource = new TestResource("asd");

        _pool.release(_key, unknownResource);

        assertThat(_pool.getObjectToLifetimeData().size(), is(0));
    }
    
    @Test(timeout = 30 * 1000)
    public void registeredObjectDisappeared_relatedLifetimeDataIsRemoved() throws InterruptedException {
    
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        @SuppressWarnings("unused")
		TestResource resource = addObject("will disappear");
        
        resource = null;
        while (_pool.getObjectToLifetimeData().size() > 0) {
        	System.gc();
        	Thread.sleep(10);
        }
    }
    
    @Test
    public void maxLifetimeIsSpecified_utilizerIsCalledForTooOldObjects() throws Exception {
    
        _settings.setMaxObjectLifetimeInSeconds(5);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource tooOldResource = new TestResource("old");
        ObjectLifetimeData<TestKey> dataWithTooOldTimestamp = new ObjectLifetimeData<TestKey>(_key)
        													  .setCreationTimeStamp(currentTime() - 6000);        
        _pool.getObjectToLifetimeData().put(tooOldResource, dataWithTooOldTimestamp);
        
        _pool.dropLifelessObjectsAndWakeupOthers();

        verify(_objectUtilizerMock).utilize(_key, tooOldResource, _pool);
    }
    
    @Test
    public void onlyMaxLifetimeIsSpecified_youngObjectsAreNotUtilized() {
    	
        _settings.setMaxObjectLifetimeInSeconds(5);
        _settings.setMaxObjectIdleTimeSpanInSeconds(null);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource youngResource = addObject("young");

        _pool.dropLifelessObjectsAndWakeupOthers();

        verify(_objectUtilizerMock, never()).utilize(_key, youngResource, _pool);
    }
    
    @Test
    public void maxIdleTimeIsSpecified_notActiveObjectsAreUtilized() {
    	
        _settings.setMaxObjectIdleTimeSpanInSeconds(5);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource notActiveResource = new TestResource("notActive");
        ObjectLifetimeData<TestKey> dataWithTooOldTimestamp = new ObjectLifetimeData<TestKey>(_key)
				  											  .setLastUsageTimeStamp(currentTime() - 6000);   
        _pool.getObjectToLifetimeData().put(notActiveResource, dataWithTooOldTimestamp);
        
        _pool.dropLifelessObjectsAndWakeupOthers();

        verify(_objectUtilizerMock).utilize(_key, notActiveResource, _pool);
    }
    
    @Test
    public void onlyMaxIdleTimeIsSpecified_activeObjectsAreNotUtilized() {
    	
        _settings.setMaxObjectIdleTimeSpanInSeconds(5);
        _settings.setMaxObjectLifetimeInSeconds(null);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource activeResource = addObject("active");

        _pool.dropLifelessObjectsAndWakeupOthers();

        verify(_objectUtilizerMock, never()).utilize(_key, activeResource, _pool);
    }
    
    @Test
    public void cleaningExecuted_usefulObjectsArePingedAndOthersAreNot() {
    	
    	_settings.setMaxObjectIdleTimeSpanInSeconds(5);
    	_settings.setMaxObjectLifetimeInSeconds(10);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        
        TestResource notActiveResource = new TestResource("notActive");
        _pool.getObjectToLifetimeData().put(notActiveResource, new ObjectLifetimeData<TestKey>(_key)
        													   .setCreationTimeStamp(currentTime() - 8*1000)
        													   .setLastUsageTimeStamp(currentTime() - 6*1000));
        TestResource tooOldResource = new TestResource("tooOld");
        _pool.getObjectToLifetimeData().put(tooOldResource, new ObjectLifetimeData<TestKey>(_key)
        													.setCreationTimeStamp(currentTime() - 12*1000));
        TestResource usefulResource1 = addObject("useful1");
        TestResource usefulResource2 = addObject("useful2");
        
        _pool.dropLifelessObjectsAndWakeupOthers();
        
        verify(_successfulObjectActionsMock, never()).ping(notActiveResource);
        verify(_successfulObjectActionsMock, never()).ping(tooOldResource);
        verify(_successfulObjectActionsMock).ping(usefulResource1);
        verify(_successfulObjectActionsMock).ping(usefulResource2);
    }
    
    @Test
    public void cleaningExecutedAndPingFailed_objectIsUtilizedAndCleaningProceeds() {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock, _failingOnPingObjectActionsMock, _objectUtilizerMock);
        TestResource object1 = addObject("something1");
        TestResource object2 = addObject("something2");

        _pool.dropLifelessObjectsAndWakeupOthers();

        verify(_objectUtilizerMock).utilize(_key, object1, _pool);
        verify(_objectUtilizerMock).utilize(_key, object2, _pool);
    }
    
    @Test
    public void cleaningIsCalledPeriodically() throws InterruptedException {
    	
        _settings.setTimeSpanBetweenRevivalsInSeconds(1);
        _settings.setMaxObjectLifetimeInSeconds(1);
        _settings.setMaxObjectIdleTimeSpanInSeconds(null);
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);

        TestResource tooOld = new TestResource("too old");
        _pool.getObjectToLifetimeData().put(tooOld, new ObjectLifetimeData<TestKey>(_key)
        											.setCreationTimeStamp(currentTime() - 10 * 1000));
        TestResource becomingOld = new TestResource("becoming old");
        _pool.getObjectToLifetimeData().put(becomingOld, new ObjectLifetimeData<TestKey>(_key)
														 .setCreationTimeStamp(currentTime() - 1000));
        TestResource foreverYoung = new TestResource("forever young");
        _pool.getObjectToLifetimeData().put(foreverYoung, new ObjectLifetimeData<TestKey>(_key)
				 										  .setCreationTimeStamp(currentTime() + 3600 * 1000));

        Thread.sleep(3000);

        verify(_objectUtilizerMock, atLeastOnce()).utilize(_key, tooOld, _pool);
        verify(_objectUtilizerMock, atLeastOnce()).utilize(_key, becomingOld, _pool);
        verify(_objectUtilizerMock, never()).utilize(_key, foreverYoung, _pool);
    }
    
    @Test
    public void basePoolThrewOnObtain_itIsRethrownAsIs() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);

        _createDelegate = key -> new TestResource("");
        _thrown.expect(ObjectCreationFailedException.class);
        _thrown.expect(new CreationFailedExceptionMatcher(_key, _createDelegate, PoolMocks.CAUSE_OF_CREATION_FAILURE));
        
        _pool.obtain(_key, _createDelegate);
    }

    @Test
    public void basePoolThrewOnRelease_itIsRethrownAsIs() throws Exception {
    	
        _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock, _successfulObjectActionsMock, _objectUtilizerMock);
        TestResource resourceToRelease = new TestResource("");

        _thrown.expect(InvalidPoolOperationException.class);
        _thrown.expect(new InvalidOperationExceptionMatcher(_key, resourceToRelease));
        
        _pool.release(_key, resourceToRelease);
    }
	
    private TestResource addObject(String resourceValue) {
    	
        TestResource resource = new TestResource(resourceValue);
        _pool.getObjectToLifetimeData().put(resource, new ObjectLifetimeData<TestKey>(_key));
        return resource;
    }
}
