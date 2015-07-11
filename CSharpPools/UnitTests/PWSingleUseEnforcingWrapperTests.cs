using System;
using System.CodeDom;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Moq;
using NUnit.Framework;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool;
using PoolsLibrary.Pool.BasicFunctionality.Item;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;
using UnitTests.TestEntities;

namespace UnitTests
{
    [TestFixture]
    public class PWSingleUseEnforcingWrapperTests
    {
        private PWObjectStateMonitoringWrapper<TestKey, TestResource> _pool;
        private TestResource _outPoolObject;

        private PWObjectStateMonitoringSettings _settings;
        private readonly TestKey _key;
        private const int _timeToleranceInMills = 50;

        private Mock<IInternalPool<TestKey, TestResource>> _baseSuccessfulPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _baseSameObjectPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _baseNoObjectPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _baseThrowingPoolMock;
        private Mock<IPoolObjectActions<TestResource>> _successfulObjectActionsMock;
        private Mock<IPoolObjectActions<TestResource>> _failingOnPingObjectActionsMock;
        private Mock<IObjectUtilizer<TestKey,TestResource>> _objectUtilizerMock;
        private readonly GoneObjectEventArgs<TestKey, TestResource> _goneObjectArgs;

        public PWSingleUseEnforcingWrapperTests()
        {
            _key = new TestKey
            {
                Identifier = 375,
            };

            _goneObjectArgs = new GoneObjectEventArgs<TestKey, TestResource>
            {
                Reporter = new object(),
                Key = _key,
                PoolObject = new TestResource("gone"),
            };
        }

        [SetUp]
        public void SetUp()
        {
            _settings = new PWObjectStateMonitoringSettings
            {
                TimeSpanBetweenRevivalsInSeconds = 5,
                MaxObjectLifetimeInSeconds = 3,
                MaxObjectIdleTimeSpanInSeconds = 2,
            };

            _baseSuccessfulPoolMock = Mocks.Pool.GetNewReturningDifferentObjects();
            _baseSameObjectPoolMock = Mocks.Pool.GetNewReturningSameObject();
            _baseNoObjectPoolMock = Mocks.Pool.GetNewReturningNoObject();
            _baseThrowingPoolMock = Mocks.Pool.GetNewThrowing();

            _successfulObjectActionsMock = Mocks.ObjectActions.GetNewSuccessful();
            _failingOnPingObjectActionsMock = Mocks.ObjectActions.GetNewFailingOnPing();

            _objectUtilizerMock = Mocks.ObjectUtilizer.GetNew();
        }

        [Test]
        public void ObtainWasCalled_BasePoolIsCalled()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            Func<TestKey, TestResource> createDelegate = key => new TestResource(string.Empty);

            _pool.TryObtain(_key, out _outPoolObject, createDelegate);

            _baseSuccessfulPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, createDelegate), Times.Once);
        }

        [Test]
        public void ObtainWasCalled_ProvidedObjectIsReturned()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            var getStatus = _pool.TryObtain(_key, out _outPoolObject, null);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo(_key.Identifier + " 1"));
        }

        [Test]
        public void ReleaseWasCalled_BasePoolIsCalled()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var resource = AddObject("asd");

            _pool.Release(_key, resource);

            _baseSuccessfulPoolMock.Verify(x => x.Release(_key, resource), Times.Once);
        }


        #region Remembering timestamps

        [Test]
        public void BasePoolProvidesObject_ObjectLifetimeDataAppears()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            _pool.TryObtain(_key, out _outPoolObject, null);

            var lifetimeData = _pool.ObjectToLifetimeData[_outPoolObject];
            Assert.That(lifetimeData.Key, Is.EqualTo(_key));
            Assert.That(lifetimeData.CreationTimeStamp, Is.EqualTo(DateTime.Now).Within(_timeToleranceInMills).Milliseconds);
            Assert.That(lifetimeData.LastUsageTimeStamp, Is.EqualTo(DateTime.Now).Within(_timeToleranceInMills).Milliseconds);
        }

        [Test]
        public void BasePoolReturnedFalse_PoolIsNotBrokenAndJustReturnsFalse()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseNoObjectPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            var getStatus = _pool.TryObtain(_key, out _outPoolObject, null);

            Assert.That(getStatus, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void AnotherObtainingOfSameObject_CreationTimestampIsNotModified()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            _pool.TryObtain(_key, out _outPoolObject, null);
            var previous = _pool.ObjectToLifetimeData[_outPoolObject].CreationTimeStamp;

            Thread.Sleep(5);
            _pool.TryObtain(_key, out _outPoolObject, null);

            var current = _pool.ObjectToLifetimeData[_outPoolObject].CreationTimeStamp;
            Assert.That(previous, Is.EqualTo(current).Within(_timeToleranceInMills).Milliseconds);
        }

        [Test]
        public void AnotherObtainingOfSameObject_LastUsageTimestampIsUpdated()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            _pool.TryObtain(_key, out _outPoolObject, null);
            var previous = _pool.ObjectToLifetimeData[_outPoolObject].LastUsageTimeStamp;

            Thread.Sleep(5);
            _pool.TryObtain(_key, out _outPoolObject, null);

            var current = _pool.ObjectToLifetimeData[_outPoolObject].LastUsageTimeStamp;
            Assert.That(current - previous >= TimeSpan.FromMilliseconds(5));
            Assert.That(previous, Is.EqualTo(current).Within(_timeToleranceInMills).Milliseconds);
        }

        [Test]
        public void AnotherObtainingOfSameObject_RememberedObjectKeyIsPreserved()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSameObjectPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            _pool.TryObtain(_key, out _outPoolObject, null);
            var previous = _pool.ObjectToLifetimeData[_outPoolObject].Key;

            _pool.TryObtain(_key, out _outPoolObject, null);

            var current = _pool.ObjectToLifetimeData[_outPoolObject].Key;
            Assert.That(previous, Is.EqualTo(current));
        }

        [Test]
        [Ignore]
        public void OnlyRevivalTimespanIsSpecified_TimestampsAreNotRememberedOnObtain()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _settings.MaxObjectLifetimeInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            _pool.TryObtain(_key, out _outPoolObject, null);

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
        }

        [Test]
        public void ReleaseWasCalled_LastUsageTimestampIsUpdated()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var resource = AddObject("asd");
            var previous = _pool.ObjectToLifetimeData[resource].LastUsageTimeStamp;

            Thread.Sleep(5);
            _pool.Release(_key, resource);

            var current = _pool.ObjectToLifetimeData[resource].LastUsageTimeStamp;
            Assert.That(current - previous >= TimeSpan.FromMilliseconds(5));
            Assert.That(previous, Is.EqualTo(current).Within(_timeToleranceInMills).Milliseconds);
        }

        [Test]
        public void ReleaseWasCalled_CreationTimestampIsNotModified()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var resource = AddObject("asd");
            var previous = _pool.ObjectToLifetimeData[resource].CreationTimeStamp;

            Thread.Sleep(5);
            _pool.Release(_key, resource);

            var current = _pool.ObjectToLifetimeData[resource].CreationTimeStamp;
            Assert.That(current, Is.EqualTo(previous));
        }

        [Test]
        public void ReleaseWasCalled_RememberedKeyIsPreserved()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var resource = AddObject("asd");
            var previous = _pool.ObjectToLifetimeData[resource].Key;

            _pool.Release(_key, resource);

            var current = _pool.ObjectToLifetimeData[resource].Key;
            Assert.That(current, Is.EqualTo(previous));
        }

        [Test]
        public void ReleasingOfUnknownObject_NotThrowsAndAddsTimestampData()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var unknownResource = new TestResource("asd");

            Assert.DoesNotThrow(() => _pool.Release(_key, unknownResource));

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(1));
            var lifetimeData = _pool.ObjectToLifetimeData[unknownResource];
            Assert.That(lifetimeData.CreationTimeStamp, Is.EqualTo(DateTime.Now).Within(_timeToleranceInMills).Milliseconds);
            Assert.That(lifetimeData.LastUsageTimeStamp, Is.EqualTo(DateTime.Now).Within(_timeToleranceInMills).Milliseconds);
        }

        [Test]
        public void ReleasingOfUnknownObject_BasePoolIsCalled()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var unknownResource = new TestResource("asd");

            _pool.Release(_key, unknownResource);

            _baseSuccessfulPoolMock.Verify(x => x.Release(_key,unknownResource), Times.Once);
        }

        [Test]
        public void OnlyRevivalTimespanIsSpecified_TimestampsAreNotRememberedOnRelease()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _settings.MaxObjectLifetimeInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var unknownResource = new TestResource("asd");

            _pool.Release(_key, unknownResource);

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
        }

        #endregion Remembering timestamps


        #region Cleaning on GoneObjectEvent

        [Test]
        public void UtilizerRaisedObjectIsGoneEvent_RelatedLifetimeDataIsRemoved()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            _pool.ObjectToLifetimeData[_goneObjectArgs.PoolObject] = new ObjectLifetimeData<TestKey>(_key);
 
            _objectUtilizerMock.Raise(x => x.ObjectIsGone += null, _goneObjectArgs);

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
        }

        [Test]
        public void UtilizerRaisedEventWithUnknownObject_PoolDoesNotThrow()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            Assert.DoesNotThrow(() => _objectUtilizerMock.Raise(x => x.ObjectIsGone += null, _goneObjectArgs));
        }

        #endregion Cleaning on GoneObjectEvent

        
        #region Periodic cleaning

        [Test]
        public void MaxLifetimeIsSpecified_TooOldObjectsAreUtilizedAndForgotten()
        {
            _settings.MaxObjectLifetimeInSeconds = 5;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var tooOldResource = new TestResource("old");
            _pool.ObjectToLifetimeData[tooOldResource] = new ObjectLifetimeData<TestKey>(_key)
            {
                CreationTimeStamp = DateTime.Now - TimeSpan.FromSeconds(6),
            };

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
            _objectUtilizerMock.Verify(x => x.Utilize(_key, tooOldResource, _pool), Times.Once);
        }

        [Test]
        public void OnlyMaxLifetimeIsSpecified_ValidYoungObjectsAreKept()
        {
            _settings.MaxObjectLifetimeInSeconds = 5;
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            AddObject("young");

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(1));
        }

        [Test]
        public void MaxIdleTimeIsSpecified_NotActiveObjectsAreUtilizedAndForgotten()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = 5;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var tooOldResource = new TestResource("notActive");
            _pool.ObjectToLifetimeData[tooOldResource] = new ObjectLifetimeData<TestKey>(_key)
            {
                LastUsageTimeStamp = DateTime.Now - TimeSpan.FromSeconds(6),
            };

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
            _objectUtilizerMock.Verify(x => x.Utilize(_key, tooOldResource, _pool), Times.Once);
        }

        [Test]
        public void OnlyMaxIdleTimeIsSpecified_ValidActiveObjectsAreKept()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = 5;
            _settings.MaxObjectLifetimeInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            AddObject("active");

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(1));
        }

        [Test]
        public void CleaningExecutedAndTimespansAreNotSpecified_InvalidObjectsAreUtilizedAndForgotten()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _settings.MaxObjectLifetimeInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var invalidResource = AddObject(Mocks.ObjectActions.SubstringOfInvalidObject);

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
            _objectUtilizerMock.Verify(x => x.Utilize(_key, invalidResource, _pool), Times.Once);
        }

        [Test]
        public void CleaningExecutedWithoutTimespans_ValidObjectsAreKept()
        {
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _settings.MaxObjectLifetimeInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            AddObject("valid");

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(1));
        }

        [Test]
        public void CleaningExecuted_UsefulObjectsArePinged()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);
            var usefulResource = AddObject("useful");

            _pool.DropLifelessObjectsAndWakeupOthers();

            _successfulObjectActionsMock.Verify(x => x.Ping(usefulResource));
        }

        [Test]
        public void CleaningExecutedAndPingFailed_ObjectIsUtilizedAndForgottenAndCleaningProceeds()
        {
            _settings.TimeSpanBetweenRevivalsInSeconds = 100;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock.Object, _failingOnPingObjectActionsMock.Object, _objectUtilizerMock.Object);
            var object1 = AddObject("something1");
            var object2 = AddObject("something2");

            _pool.DropLifelessObjectsAndWakeupOthers();

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(0));
            _objectUtilizerMock.Verify(x => x.Utilize(_key,object1,_pool));
            _objectUtilizerMock.Verify(x => x.Utilize(_key,object2,_pool));
        }

        [Test]
        public void CleaningIsCalledPeriodically()
        {
            _settings.TimeSpanBetweenRevivalsInSeconds = 1;
            _settings.MaxObjectLifetimeInSeconds = 1;
            _settings.MaxObjectIdleTimeSpanInSeconds = null;
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseSuccessfulPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            _pool.ObjectToLifetimeData[new TestResource("too old")] = new ObjectLifetimeData<TestKey>(_key)
            {
                CreationTimeStamp = DateTime.Now - TimeSpan.FromSeconds(10),
            };
            _pool.ObjectToLifetimeData[new TestResource("becoming old")] = new ObjectLifetimeData<TestKey>(_key)
            {
                CreationTimeStamp = DateTime.Now + TimeSpan.FromSeconds(1),
            };
            _pool.ObjectToLifetimeData[new TestResource("forever young")] = new ObjectLifetimeData<TestKey>(_key)
            {
                CreationTimeStamp = DateTime.Now + TimeSpan.FromHours(1),
            };

            Thread.Sleep(TimeSpan.FromSeconds(3));

            Assert.That(_pool.ObjectToLifetimeData.Count, Is.EqualTo(1));
            _objectUtilizerMock.Verify(x => x.Utilize(_key, It.IsAny<TestResource>(), _pool), Times.Exactly(2));
        }

        #endregion Periodic cleaning


        #region Exception handling

        [Test]
        public void BasePoolThrewOnObtain_ItIsRethrownAsIs()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            Assert.Throws<ObjectCreationFailedException<TestKey,TestResource>>(
                () => _pool.TryObtain(_key, out _outPoolObject, null));
        }

        [Test]
        public void BasePoolThrewOnRelease_ItIsRethrownAsIs()
        {
            _pool = new PWObjectStateMonitoringWrapper<TestKey, TestResource>(_settings, _baseThrowingPoolMock.Object, _successfulObjectActionsMock.Object, _objectUtilizerMock.Object);

            _outPoolObject = new TestResource(string.Empty);
            Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _pool.Release(_key, _outPoolObject));
        }

        #endregion Exception handling


        private TestResource AddObject(string resourceValue)
        {
            var resource = new TestResource(resourceValue);
            _pool.ObjectToLifetimeData[resource] = new ObjectLifetimeData<TestKey>(_key);
            return resource;
        }
    }
}
