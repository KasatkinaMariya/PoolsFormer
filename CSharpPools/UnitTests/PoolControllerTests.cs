using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Moq;
using NUnit.Framework;
using PoolsLibrary;
using PoolsLibrary.Controller;
using PoolsLibrary.Pool;
using PoolsLibrary.Pool.BasicFunctionality.Item;
using UnitTests.TestEntities;
using UnitTests.Utils;

namespace UnitTests
{
    [TestFixture]
    public class PoolControllerTests
    {
        private PoolController<TestKey, TestResource> _controller;
        private TestResource _outPoolObject;

        private readonly PoolControllerSettings _settings;
        private readonly TestKey _key;
        private readonly DirectionIfNoObjectIsAvailable<TestKey, TestResource> _noObjectDirection;

        private Mock<IInternalPool<TestKey, TestResource>> _successPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _noObjectPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _noObjectThreeTimesPoolMock;
        private Mock<IInternalPool<TestKey, TestResource>> _throwingPoolMock;

        public PoolControllerTests()
        {
            _key = new TestKey
            {
                Identifier = 48635,
            };
            _settings = new PoolControllerSettings
            {
                CallingReleaseOperationWillHappen = true,
            };
            _noObjectDirection = new DirectionIfNoObjectIsAvailable<TestKey, TestResource>();
        }

        [SetUp]
        public void SetUp()
        {
            _outPoolObject = null;

            _successPoolMock = Mocks.Pool.GetNewReturningDifferentObjects();
            _noObjectPoolMock = Mocks.Pool.GetNewReturningNoObject();
            _noObjectThreeTimesPoolMock = Mocks.Pool.GetNewReturningNoObjectThreeTimes();
            _throwingPoolMock = Mocks.Pool.GetNewThrowing();
        }


        #region TryObtain

        [Test]
        public void ObtainWasCalled_PoolIsCalled_ProvidedObjectIsReturned()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);

            var getStatus = _controller.Obtain(_key, out _outPoolObject, null);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo(_key.Identifier + " 1"));
            _successPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Once);
        }

        [Test]
        public void NumberOfAttemptsIsSpecified_PoolIsRecalledAndFinalResultIsGood()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectThreeTimesPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 5;
            var getStatus = _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo(Mocks.Pool.ResourceValueAfterWaitings));
            _noObjectThreeTimesPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Exactly(4));
        }

        [Test]
        public void ControllerPerformedSpecifiedNumberOfAttempts_ControllerStopsReattempting()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectPoolMock.Object);
            _noObjectDirection.AttemptsNumber = 7;

            _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);

            _noObjectPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, It.IsAny<Func<TestKey, TestResource>>()), Times.Exactly(7));
        }

        [Test]
        public void NullNoObjectDirection_ControllerPerformsOneAttemptsWithoutCreateDelegate()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectPoolMock.Object);

            _controller.Obtain(_key, out _outPoolObject, null);

            _noObjectPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Once);
        }

        [Test]
        public void AllAttemptsWereTriedAndPoolDidNotFindAvailableObject_ControllerReturnsDefault()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectPoolMock.Object);
            _noObjectDirection.AttemptsNumber = 3;
            _noObjectDirection.OneIntervalBetweenAttemptsInSeconds = 0;

            var getStatus = _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);

            Assert.That(getStatus, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void CreateDelegateIsSpecified_PoolIsCalledWithIt_ButOnlyLastTime()
        {
            Func<TestKey, TestResource> createDelegate = key => new TestResource("on fifth call");
            int curCallNumber = 0;
            TestResource orderControllingOutResource;
            var callsOrderControllingPoolMock = new Mock<IPool<TestKey, TestResource>>(MockBehavior.Strict);
            callsOrderControllingPoolMock.Setup(x => x.TryObtain(_key, out orderControllingOutResource, null))
                                         .Returns(false)
                                         .Callback(() => Assert.That(++curCallNumber, Is.InRange(1, 4)));
            callsOrderControllingPoolMock.Setup(x => x.TryObtain(_key, out orderControllingOutResource, createDelegate))
                                         .OutCallback((TestKey key, out TestResource outResource, Func<TestKey,TestResource> createDel)
                                             => outResource = createDelegate(key))
                                         .Returns<TestKey, TestResource, Func<TestKey, TestResource>>((key, outResource, createDel) => true)
                                         .Callback(() => Assert.That(++curCallNumber, Is.EqualTo(5)));
            _controller = new PoolController<TestKey, TestResource>(_settings, callsOrderControllingPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 5;
            _noObjectDirection.CreateDelegateIfNoObjectIsAvailable = createDelegate;
            var getStatus = _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("on fifth call"));
            callsOrderControllingPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Exactly(4));
            callsOrderControllingPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, createDelegate), Times.Once);
            callsOrderControllingPoolMock.Verify(x => x.TryObtain(It.IsAny<TestKey>(), out _outPoolObject, It.IsAny<Func<TestKey, TestResource>>()),
                                                 Times.Exactly(5));
        }

        [Test]
        public void ReattempsIntervalIsSpecified_ControllerWaitsBeforeReattempt()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectThreeTimesPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 8;
            _noObjectDirection.OneIntervalBetweenAttemptsInSeconds = 1;

            var stopwatch = Stopwatch.StartNew();
            var getStatus = _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);
            stopwatch.Stop();

            Assert.That(stopwatch.ElapsedMilliseconds, Is.InRange(2900, 4000));
            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo(Mocks.Pool.ResourceValueAfterWaitings));
        }

        [Test]
        public void ReattempsIntervalIsEqualToZero_ControllerDoesNotWait()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _noObjectThreeTimesPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 8;
            _noObjectDirection.OneIntervalBetweenAttemptsInSeconds = 0;

            var stopwatch = Stopwatch.StartNew();
            var getStatus = _controller.Obtain(_key, out _outPoolObject, _noObjectDirection);
            stopwatch.Stop();

            Assert.That(stopwatch.ElapsedMilliseconds, Is.LessThan(100));
            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo(Mocks.Pool.ResourceValueAfterWaitings));
        }

        [Test]
        public void PoolThrew_ControllerStopsReattemptingAndThrowDetailedPoolException()
        {
            _controller = new PoolController<TestKey, TestResource>(_settings, _throwingPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 3;
            var internalErrorException = Assert.Throws<PoolException<TestKey>>(() =>
                _controller.Obtain(_key, out _outPoolObject, _noObjectDirection));

            var expectedMessage = string.Format("Something failed during attempt #1 of obtaining object with key='{0}'. " +
                                                "Look at inner exception for details", _key);
            Assert.That(internalErrorException.Message, Is.EqualTo(expectedMessage));
            Assert.That(internalErrorException.Key, Is.EqualTo(_key));
            Assert.That(internalErrorException.InnerException, Is.TypeOf<ObjectCreationFailedException<TestKey, TestResource>>());
            _throwingPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Once);
        }

        #endregion TryObtain


        #region Release

        [Test]
        public void ReleasingWasPromised_ControllerCanRecallKeyForEachObtainedValue()
        {
            _settings.CallingReleaseOperationWillHappen = true;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);
            TestResource outResource1;
            TestResource outResource2;
            TestResource outResource3;
            var key1 = new TestKey { Identifier = 100 };
            var key2 = new TestKey { Identifier = 200 };

            _controller.Obtain(key1, out outResource1, null);
            _controller.Obtain(key1, out outResource2, null);
            _controller.Obtain(key2, out outResource3, null);

            Assert.That(_controller.ObtainedObjectToItsKey[outResource1], Is.EqualTo(key1));
            Assert.That(_controller.ObtainedObjectToItsKey[outResource2], Is.EqualTo(key1));
            Assert.That(_controller.ObtainedObjectToItsKey[outResource3], Is.EqualTo(key2));
        }

        [Test]
        public void ReleaseWasCalled_PoolIsCalled()
        {
            _settings.CallingReleaseOperationWillHappen = true;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);
            TestResource outResource1;
            TestResource outResource2;
            TestResource outResource3;
            var key1 = new TestKey { Identifier = 100 };
            var key2 = new TestKey { Identifier = 200 };

            _controller.Obtain(key1, out outResource1, null);
            _controller.Obtain(key1, out outResource2, null);
            _controller.Obtain(key2, out outResource3, null);
            _controller.Release(outResource1);
            _controller.Release(outResource2);
            _controller.Release(outResource3);

            _successPoolMock.Verify(x => x.Release(key1, outResource1), Times.Once);
            _successPoolMock.Verify(x => x.Release(key1, outResource2), Times.Once);
            _successPoolMock.Verify(x => x.Release(key2, outResource3), Times.Once);
        }

        [Test]
        public void ReleaseWasCalled_ControllerForgotsObjectToKeyMapping()
        {
            _settings.CallingReleaseOperationWillHappen = true;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);

            _controller.Obtain(_key, out _outPoolObject, null);
            _controller.Release(_outPoolObject);

            Assert.That(_controller.ObtainedObjectToItsKey.Count, Is.EqualTo(0));
        }

        [Test]
        public void ReleasingWasNotPromised_ControllerDoesNotRememberObtainedObjects()
        {
            _settings.CallingReleaseOperationWillHappen = false;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);

            _controller.Obtain(_key, out _outPoolObject, null);

            Assert.That(_controller.ObtainedObjectToItsKey.Count, Is.EqualTo(0));
        }

        #endregion Release


        #region UserFriendly

        [Test]
        public void ReleasingWasNotPromisedButWasCalled_ExceptionWithHintToPromiseReleasingIsThrown()
        {
            _settings.CallingReleaseOperationWillHappen = false;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);

            var resource = new TestResource(string.Empty);
            var hintException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _controller.Release(resource));

            var expectedMessage = "In order to release objects promise it by setting " +
                                  "PoolControllerSettings.CallingReleaseOperationWillHappen to " +
                                  "true. Currently it's false";
            Assert.That(hintException.Message, Is.EqualTo(expectedMessage));
            Assert.That(hintException.Key, Is.Null);
            Assert.That(hintException.Object, Is.EqualTo(resource));
        }

        [Test]
        public void ReleasingOfNotObtainedObject_DetailedExceptionIsThrown()
        {
            _settings.CallingReleaseOperationWillHappen = true;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);

            var resource = new TestResource(string.Empty);
            var hintException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _controller.Release(resource));

            Assert.That(hintException.Message, Is.EqualTo("Only obtained objects are allowed to be released"));
            Assert.That(hintException.Key, Is.Null);
            Assert.That(hintException.Object, Is.EqualTo(resource));
        }

        [Test]
        public void ReleasingWasNotPromised_ControllerIgnoresDirectionToWaitAndOrderPoolToCreateNewObject()
        {
            _settings.CallingReleaseOperationWillHappen = false;
            _controller = new PoolController<TestKey, TestResource>(_settings, _successPoolMock.Object);
            Func<TestKey, TestResource> createDelegate = key => new TestResource(Mocks.Pool.ResourceValueAfterWaitings);
            var directionToWaitForAvailableObject = new DirectionIfNoObjectIsAvailable<TestKey, TestResource>
            {
                AttemptsNumber = 5,
                OneIntervalBetweenAttemptsInSeconds = 10,
                CreateDelegateIfNoObjectIsAvailable = createDelegate,
            };

            _controller.Obtain(_key, out _outPoolObject, directionToWaitForAvailableObject);

            _successPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, createDelegate), Times.Once);
            _successPoolMock.Verify(x => x.TryObtain(_key, out _outPoolObject, null), Times.Never);
        }

        [Test]
        public void ReleasingWasNotPromisedAndPoolThrew_AttemptsNumberInExceptionIsEqualToOne()
        {
            _settings.CallingReleaseOperationWillHappen = false;
            _controller = new PoolController<TestKey, TestResource>(_settings, _throwingPoolMock.Object);

            _noObjectDirection.AttemptsNumber = 8;
            _noObjectDirection.OneIntervalBetweenAttemptsInSeconds = 0;
            var internalErrorException = Assert.Throws<PoolException<TestKey>>(() =>
                _controller.Obtain(_key, out _outPoolObject, _noObjectDirection));

            var expectedMessage = string.Format("Something failed during attempt #1 of obtaining object with key='{0}'. " +
                                                "Look at inner exception for details", _key);
            Assert.That(internalErrorException.Message, Is.EqualTo(expectedMessage));
        }

        #endregion UserFriendly
    }
}
