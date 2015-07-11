using System;
using System.IO;
using System.Net;
using Moq;
using NUnit.Framework;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectActions.Notification;
using UnitTests.TestEntities;

namespace UnitTests
{
    [TestFixture]
    public class PoolObjectActionsTests
    {
        private InterfaceRealizingEntity _bigInterfaceRealizingObject;
        private InterfaceRealizingEntity _smallInterfaceRealizingObject;
        private ChildOfInterfaceRealizingEntity _bigChildObject;
        private ChildOfInterfaceRealizingEntity _smallChildObject;

        private AutonomousEntity _bigAutonomousObject;
        private AutonomousEntity _smallAutonomousObject;
        private Action<object> _standalonePingImplementation;
        private bool _standalonePingWasCalled;
        
        private ObjectActionsBasedOnDelegateOrInterface<TestResource> _actions;
        private ExplicitlyDefinedObjectActions<TestResource> _normalDelegates;
        private ExplicitlyDefinedObjectActions<TestResource> _throwingDelegates;

        private readonly TestResource _fakeResource = new TestResource("fake");
        private Mock<INotifier> _notifierMock;

        [TestFixtureSetUp]
        public void SetUpFixture()
        {
            _standalonePingImplementation = x => _standalonePingWasCalled = true;

            _normalDelegates = new ExplicitlyDefinedObjectActions<TestResource>
            {
                IsValidDelegate = resource => resource.Value.Contains(Mocks.ObjectActions.SubstringOfInvalidObject),
                PingDelegate = resource => { },
                ResetDelegate = resource => { },
                DisposeDelegate = resource => { }
            };

            _throwingDelegates = new ExplicitlyDefinedObjectActions<TestResource>
            {
                IsValidDelegate = resource => { throw new WebException(); },
                PingDelegate = resource => { throw new TimeoutException(); },
                ResetDelegate = resource => { throw new IndexOutOfRangeException(); },
                DisposeDelegate = resource => { throw new AccessViolationException(); }
            };
        }

        [SetUp]
        public void SetUp()
        {
            _bigInterfaceRealizingObject = new InterfaceRealizingEntity(5);
            _smallInterfaceRealizingObject = new InterfaceRealizingEntity(3);
            _bigChildObject = new ChildOfInterfaceRealizingEntity(5);
            _smallChildObject = new ChildOfInterfaceRealizingEntity(3);

            _bigAutonomousObject = new AutonomousEntity(5);
            _smallAutonomousObject = new AutonomousEntity(3);
            _standalonePingWasCalled = false;

             _notifierMock = Mocks.Notifier.GetNew();  
             InitActionsWith(_normalDelegates);       
        }

        private void InitActionsWith(ExplicitlyDefinedObjectActions<TestResource> delegates)
        {
            _actions = new ObjectActionsBasedOnDelegateOrInterface<TestResource>(delegates, _notifierMock.Object);
        }

        #region Delegates priority
        [Test]
        public void InterfaceIsImplementedAndNoDelegateIsDefined_InterfaceImplementationIsUsed()
        {
            var overridingActions = new ExplicitlyDefinedObjectActions<InterfaceRealizingEntity>();
            var actions = new ObjectActionsBasedOnDelegateOrInterface<InterfaceRealizingEntity>(overridingActions,_notifierMock.Object);

            var bigAnswer = actions.IsValid(_bigInterfaceRealizingObject);
            var smallAnswer = actions.IsValid(_smallInterfaceRealizingObject);
            actions.Ping(_bigInterfaceRealizingObject);

            Assert.That(bigAnswer, Is.True);
            Assert.That(smallAnswer, Is.False);
            Assert.That(_bigInterfaceRealizingObject.PingImplementationWasCalled, Is.True);
        }

        [Test]
        public void BothInterfaceImplementationAndDelegateAreDefined_DelegateIsUsed()
        {
            var overridingActions = new ExplicitlyDefinedObjectActions<InterfaceRealizingEntity>
            {
                IsValidDelegate = x => x.Value < 4,
                PingDelegate = _standalonePingImplementation,
            };
            var actions = new ObjectActionsBasedOnDelegateOrInterface<InterfaceRealizingEntity>(overridingActions, _notifierMock.Object);

            var bigAnswer = actions.IsValid(_bigInterfaceRealizingObject);
            var smallAnswer = actions.IsValid(_smallInterfaceRealizingObject);
            actions.Ping(_bigInterfaceRealizingObject);

            Assert.That(bigAnswer, Is.False);
            Assert.That(smallAnswer, Is.True);
            Assert.That(_bigInterfaceRealizingObject.PingImplementationWasCalled, Is.False);
            Assert.That(_standalonePingWasCalled, Is.True);
        }

        [Test]
        public void NoInterfaceImplementationButDelegateIsDefined_DelegateIsUsed()
        {
            var overridingActions = new ExplicitlyDefinedObjectActions<AutonomousEntity>
            {
                IsValidDelegate = x => x.Value < 4,
                PingDelegate = _standalonePingImplementation,
            };
            var actions = new ObjectActionsBasedOnDelegateOrInterface<AutonomousEntity>(overridingActions, _notifierMock.Object);

            var bigAnswer = actions.IsValid(_bigAutonomousObject);
            var smallAnswer = actions.IsValid(_smallAutonomousObject);
            actions.Ping(_bigAutonomousObject);

            Assert.That(bigAnswer, Is.False);
            Assert.That(smallAnswer, Is.True);
            Assert.That(_standalonePingWasCalled, Is.True);
        }

        [Test]
        public void NeitherInterfaceImplementationNorDelegate_EmptyMethodRealizationIsUsed()
        {
            var overridingActions = new ExplicitlyDefinedObjectActions<AutonomousEntity>();
            var actions = new ObjectActionsBasedOnDelegateOrInterface<AutonomousEntity>(overridingActions, _notifierMock.Object);

            var bigAnswer = actions.IsValid(_bigAutonomousObject);
            var smallAnswer = actions.IsValid(_smallAutonomousObject);
            actions.Ping(_bigAutonomousObject);

            Assert.That(bigAnswer, Is.True);
            Assert.That(smallAnswer, Is.True);
            Assert.That(_standalonePingWasCalled, Is.False);
        }

        [Test]
        public void InterfaceIsImplementedInParentClass_ItIsDetectedAndImplementationIsUsed()
        {
            var overridingActions = new ExplicitlyDefinedObjectActions<ChildOfInterfaceRealizingEntity>();
            var actions = new ObjectActionsBasedOnDelegateOrInterface<ChildOfInterfaceRealizingEntity>(overridingActions, _notifierMock.Object);

            var bigAnswer = actions.IsValid(_bigChildObject);
            var smallAnswer = actions.IsValid(_smallChildObject);
            actions.Ping(_bigChildObject);

            Assert.That(bigAnswer, Is.True);
            Assert.That(smallAnswer, Is.False);
            Assert.That(_bigChildObject.PingImplementationWasCalled, Is.True);
        }
        #endregion Delegates priority
        
        #region Exception => not throw, return false, notify
        [Test]
        public void ValidationDelegateThrew_IsValidReturnsFalse()
        {
            InitActionsWith(_throwingDelegates);

            var isValidStatus = _actions.IsValid(_fakeResource);

            Assert.That(isValidStatus, Is.False);
        }

        [Test]
        public void ValidationDelegateThrew_NotifierIsCalled()
        {
            InitActionsWith(_throwingDelegates);

            _actions.IsValid(_fakeResource);

            var expectedErrorData = new UserDefinedActionError<TestResource>
            {
                UserDefinedActionType = UserDefinedActionType.CheckingValidness,
                Object = _fakeResource,
                Exception = new WebException(),
            };
            _notifierMock.Verify(x => x.Notify(expectedErrorData), Times.Once);
        }

        [Test]
        public void PingDelegateThrew_PingReturnsFalse()
        {
            InitActionsWith(_throwingDelegates);

            var pingStatus = _actions.Ping(_fakeResource);

            Assert.That(pingStatus, Is.False);
        }

        [Test]
        public void PingDelegateWorkedNormally_PingReturnsTrue()
        {
            var pingStatus = _actions.Ping(_fakeResource);

            Assert.That(pingStatus, Is.True);
        }

        [Test]
        public void PingDelegateThrew_NotifierIsCalled()
        {
            InitActionsWith(_throwingDelegates);

            _actions.Ping(_fakeResource);

            var expectedErrorData = new UserDefinedActionError<TestResource>
            {
                UserDefinedActionType = UserDefinedActionType.Pinging,
                Object = _fakeResource,
                Exception = new TimeoutException(),
            };
            _notifierMock.Verify(x => x.Notify(expectedErrorData), Times.Once);
        }

        [Test]
        public void DisposeDelegateThrew_NotifierIsCalled()
        {
            InitActionsWith(_throwingDelegates);

            _actions.Dispose(_fakeResource);

            var expectedErrorData = new UserDefinedActionError<TestResource>
            {
                UserDefinedActionType = UserDefinedActionType.Disposing,
                Object = _fakeResource,
                Exception = new AccessViolationException(),
            };
            _notifierMock.Verify(x => x.Notify(expectedErrorData), Times.Once);
        }

        [Test]
        public void ResetDelegateThrew_ResetReturnsFalse()
        {
            InitActionsWith(_throwingDelegates);

            var resetStatus = _actions.Reset(_fakeResource);

            Assert.That(resetStatus, Is.False);
        }

        [Test]
        public void ResetDelegateWorkedNormally_ResetReturnsTrue()
        {
            var resetStatus = _actions.Reset(_fakeResource);

            Assert.That(resetStatus, Is.True);
        }

        [Test]
        public void ResetDelegateThrew_NotifierIsCalled()
        {
            InitActionsWith(_throwingDelegates);

            _actions.Reset(_fakeResource);

            var expectedErrorData = new UserDefinedActionError<TestResource>
            {
                UserDefinedActionType = UserDefinedActionType.Resetting,
                Object = _fakeResource,
                Exception = new IndexOutOfRangeException(),
            };
            _notifierMock.Verify(x => x.Notify(expectedErrorData), Times.Once);
        }
        #endregion Exception => not throw, return false, notify
        
        #region Helpful test entities
        private class AutonomousEntity
        {
            public int Value { get; private set; }

            public AutonomousEntity(int value)
            {
                Value = value;
            }
        }

        private class InterfaceRealizingEntity : IValidnessCheckable, IPingable
        {
            public int Value { get; private set; }
            public bool PingImplementationWasCalled { get; private set; }

            public InterfaceRealizingEntity(int value)
            {
                Value = value;
            }

            public bool IsValid()
            {
                return Value >= 4;
            }

            public void Ping()
            {
                PingImplementationWasCalled = true;
            }
        }

        private class ChildOfInterfaceRealizingEntity : InterfaceRealizingEntity
        {
            public ChildOfInterfaceRealizingEntity(int value)
                : base(value)
            {
            }
        }
        #endregion Helpful test entities
    }
}
