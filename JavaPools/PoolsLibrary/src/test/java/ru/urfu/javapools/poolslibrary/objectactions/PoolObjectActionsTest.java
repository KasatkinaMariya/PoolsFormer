package ru.urfu.javapools.poolslibrary.objectactions;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeoutException;
import java.security.acl.NotOwnerException;

import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.RefreshFailedException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ru.urfu.javapools.poolslibrary.mocks.ObjectActionsMocks;
import ru.urfu.javapools.poolslibrary.objectactions.notification.INotifier;
import ru.urfu.javapools.poolslibrary.objectactions.notification.UserDefinedActionError;
import ru.urfu.javapools.poolslibrary.objectactions.notification.UserDefinedActionType;
import ru.urfu.javapools.poolslibrary.testentities.TestResource;

@RunWith(MockitoJUnitRunner.class)
public class PoolObjectActionsTest {

    private InterfaceRealizingEntity _bigInterfaceRealizingObject;
    private InterfaceRealizingEntity _smallInterfaceRealizingObject;
    private ExplicitlyDefinedObjectActions<InterfaceRealizingEntity> _interfaceEntityOverridingActions;
    private ObjectActionsBasedOnDelegateOrInterface<InterfaceRealizingEntity> _interfaceEntityActions;
    
    private ChildOfInterfaceRealizingEntity _bigChildObject;
    private ChildOfInterfaceRealizingEntity _smallChildObject;
    private ExplicitlyDefinedObjectActions<ChildOfInterfaceRealizingEntity> _childObjectOverridingActions;
    private ObjectActionsBasedOnDelegateOrInterface<ChildOfInterfaceRealizingEntity> _childObjectActions;
    
    private AutonomousEntity _bigAutonomousObject;
    private AutonomousEntity _smallAutonomousObject;
    private boolean _standalonePingWasCalled;
    private ExplicitlyDefinedObjectActions<AutonomousEntity> _autonomousEntityOverridingActions;
    private ObjectActionsBasedOnDelegateOrInterface<AutonomousEntity> _autonomousEntityActions;
    
    private ObjectActionsBasedOnDelegateOrInterface<TestResource> _actions;
    private ExplicitlyDefinedObjectActions<TestResource> _normalDelegates;
    private ExplicitlyDefinedObjectActions<TestResource> _throwingDelegates;

    private final TestResource _fakeResource = new TestResource("fake");
    @Mock private INotifier _notifierMock;
	
    public PoolObjectActionsTest() {

        _normalDelegates = new ExplicitlyDefinedObjectActions<TestResource>() {{
        	setIsValidDelegate(x -> x.getValue().contains(ObjectActionsMocks.SUBSTRING_OF_INVALID_OBJECT));
        	setPingDelegate(x -> {});
        	setResetDelegate(x -> {});
        	setCloseDelegate(x -> {});
        }};
        
        _throwingDelegates = new ExplicitlyDefinedObjectActions<TestResource>() {{
        	setIsValidDelegate(x -> { throw new TimeoutException(); });
        	setPingDelegate(x -> { throw new RefreshFailedException(); });
        	setResetDelegate(x -> { throw new UnsupportedCallbackException(null); });
        	setCloseDelegate(x -> { throw new NotOwnerException(); });
        }};
    }
    
    @Before
    public void before() {
    	
        _bigInterfaceRealizingObject = new InterfaceRealizingEntity(5);
        _smallInterfaceRealizingObject = new InterfaceRealizingEntity(3);
        _bigChildObject = new ChildOfInterfaceRealizingEntity(5);
        _smallChildObject = new ChildOfInterfaceRealizingEntity(3);

        _bigAutonomousObject = new AutonomousEntity(5);
        _smallAutonomousObject = new AutonomousEntity(3);
        _standalonePingWasCalled = false;

         _notifierMock = mock(INotifier.class);
         initActionsWith(_normalDelegates);       
    }
    
    private void initActionsWith(ExplicitlyDefinedObjectActions<TestResource> delegates) {
        _actions = new ObjectActionsBasedOnDelegateOrInterface<TestResource>(TestResource.class, delegates, _notifierMock);
    }
    
    @Test
    public void interfaceIsImplementedAndNoDelegateIsDefined_interfaceImplementationIsUsed() {
    	
        _interfaceEntityOverridingActions = new ExplicitlyDefinedObjectActions<InterfaceRealizingEntity>();
        _interfaceEntityActions = new ObjectActionsBasedOnDelegateOrInterface<InterfaceRealizingEntity>(
        		InterfaceRealizingEntity.class, _interfaceEntityOverridingActions);

        boolean bigAnswer = _interfaceEntityActions.isValid(_bigInterfaceRealizingObject);
        boolean smallAnswer = _interfaceEntityActions.isValid(_smallInterfaceRealizingObject);
        _interfaceEntityActions.ping(_bigInterfaceRealizingObject);

        assertThat(bigAnswer, is(true));
        assertThat(smallAnswer, is(false));
        assertThat(_bigInterfaceRealizingObject.pingWasCalled(), is(true));
    }
    
    @Test
    public void bothInterfaceImplementationAndDelegateAreDefined_delegateIsUsed() {
    	
        _interfaceEntityOverridingActions = new ExplicitlyDefinedObjectActions<InterfaceRealizingEntity>() {{
        	setIsValidDelegate(x -> x.getValue() < 4);
        	setPingDelegate(x -> _standalonePingWasCalled = true);
        }};
        _interfaceEntityActions = new ObjectActionsBasedOnDelegateOrInterface<InterfaceRealizingEntity>(
        		InterfaceRealizingEntity.class, _interfaceEntityOverridingActions);
    	
        boolean bigAnswer = _interfaceEntityActions.isValid(_bigInterfaceRealizingObject);
        boolean smallAnswer = _interfaceEntityActions.isValid(_smallInterfaceRealizingObject);
        _interfaceEntityActions.ping(_bigInterfaceRealizingObject);

        assertThat(bigAnswer, is(false));
        assertThat(smallAnswer, is(true));
        assertThat(_bigInterfaceRealizingObject.pingWasCalled(), is(false));
        assertThat(_standalonePingWasCalled, is(true));
    }
    
    @Test
    public void noInterfaceImplementationButDelegateIsDefined_delegateIsUsed() {
    	
    	_autonomousEntityOverridingActions = new ExplicitlyDefinedObjectActions<AutonomousEntity>() {{
        	setIsValidDelegate(x -> x.getValue() < 4);
        	setPingDelegate(x -> _standalonePingWasCalled = true);
        }};
        _autonomousEntityActions = new ObjectActionsBasedOnDelegateOrInterface<AutonomousEntity>(
        		AutonomousEntity.class, _autonomousEntityOverridingActions);

        boolean bigAnswer = _autonomousEntityActions.isValid(_bigAutonomousObject);
        boolean smallAnswer = _autonomousEntityActions.isValid(_smallAutonomousObject);
        _autonomousEntityActions.ping(_bigAutonomousObject);

        assertThat(bigAnswer, is(false));
        assertThat(smallAnswer, is(true));
        assertThat(_standalonePingWasCalled, is(true));
    }
    
    @Test
    public void neitherInterfaceImplementationNorDelegate_emptyMethodRealizationIsUsed() {
    	
    	_autonomousEntityOverridingActions = new ExplicitlyDefinedObjectActions<AutonomousEntity>();
        _autonomousEntityActions = new ObjectActionsBasedOnDelegateOrInterface<AutonomousEntity>(
        		AutonomousEntity.class, _autonomousEntityOverridingActions);

        boolean bigAnswer = _autonomousEntityActions.isValid(_bigAutonomousObject);
        boolean smallAnswer = _autonomousEntityActions.isValid(_smallAutonomousObject);
        _autonomousEntityActions.ping(_bigAutonomousObject);

        assertThat(bigAnswer, is(true));
        assertThat(smallAnswer, is(true));
        assertThat(_standalonePingWasCalled, is(false));
    }
    
    @Test
    public void interfaceIsImplementedInParentClass_itIsDetectedAndImplementationIsUsed() {
    	
        _childObjectOverridingActions = new ExplicitlyDefinedObjectActions<ChildOfInterfaceRealizingEntity>();
        _childObjectActions = new ObjectActionsBasedOnDelegateOrInterface<ChildOfInterfaceRealizingEntity>(
        		ChildOfInterfaceRealizingEntity.class, _childObjectOverridingActions);
    	
        boolean  bigAnswer = _childObjectActions.isValid(_bigChildObject);
        boolean  smallAnswer = _childObjectActions.isValid(_smallChildObject);
        _childObjectActions.ping(_bigChildObject);

        assertThat(bigAnswer, is(true));
        assertThat(smallAnswer, is(false));
        assertThat(_bigChildObject.pingWasCalled(), is(true));
    }
    
    @Test
    public void validationDelegateThrew_isValidReturnsFalse() {
    	
        initActionsWith(_throwingDelegates);

        boolean isValidStatus = _actions.isValid(_fakeResource);

        assertThat(isValidStatus, is(false));
    }
    
    @Test
    public void validationDelegateThrew_notifierIsCalled() {
    	
        initActionsWith(_throwingDelegates);

        _actions.isValid(_fakeResource);

        UserDefinedActionError<TestResource> expectedErrorData = new UserDefinedActionError<TestResource>() {{
        	setUserDefinedActionType(UserDefinedActionType.CHECKING_VALIDNESS);
        	setObject(_fakeResource);
        	setException(new TimeoutException());
        }};
        verify(_notifierMock).notify(expectedErrorData);
    }
    
    @Test
    public void pingDelegateThrew_pingReturnsFalse() {
    	
        initActionsWith(_throwingDelegates);

        boolean pingStatus = _actions.ping(_fakeResource);

        assertThat(pingStatus, is(false));
    }
    
    @Test
    public void pingDelegateWorkedNormally_pingReturnsTrue() {
    	
    	boolean pingStatus = _actions.ping(_fakeResource);

        assertThat(pingStatus, is(true));
    }
    
    @Test
    public void pingDelegateThrew_notifierIsCalled() {
    	
        initActionsWith(_throwingDelegates);

        _actions.ping(_fakeResource);

        UserDefinedActionError<TestResource> expectedErrorData = new UserDefinedActionError<TestResource>() {{
        	setUserDefinedActionType(UserDefinedActionType.PINGING);
        	setObject(_fakeResource);
        	setException(new RefreshFailedException());
        }};
        verify(_notifierMock).notify(expectedErrorData);
    }
    
    @Test
    public void resetDelegateThrew_resetReturnsFalse() {
    	
        initActionsWith(_throwingDelegates);

        boolean resetStatus = _actions.reset(_fakeResource);

        assertThat(resetStatus, is(false));
    }
    
    @Test
    public void resetDelegateWorkedNormally_resetReturnsTrue() {
    	
        boolean resetStatus = _actions.reset(_fakeResource);

        assertThat(resetStatus, is(true));
    }
    
    @Test
    public void resetDelegateThrew_notifierIsCalled() {
    	
        initActionsWith(_throwingDelegates);

        _actions.reset(_fakeResource);
        
        UserDefinedActionError<TestResource> expectedErrorData = new UserDefinedActionError<TestResource>() {{
        	setUserDefinedActionType(UserDefinedActionType.RESETTING);
        	setObject(_fakeResource);
        	setException(new UnsupportedCallbackException(null));
        }};
        verify(_notifierMock).notify(expectedErrorData);
    }
}

