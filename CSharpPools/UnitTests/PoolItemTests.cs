using System;
using System.Collections.Generic;
using System.IO.IsolatedStorage;
using System.Linq;
using System.Linq.Expressions;
using System.Net.NetworkInformation;
using System.Text;
using System.Threading.Tasks;
using Moq;
using NUnit.Framework;
using PoolsLibrary;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality.Item;
using PoolsLibrary.Pool.BasicFunctionality.Storages;
using UnitTests.TestEntities;

namespace UnitTests
{
    [TestFixture]
    public class PoolItemTests
    {
        private PoolItem<TestKey, TestResource> _poolItem;
        private TestResource _outPoolObject;
        private TestResource _anotherOutPoolObject;

        private readonly PoolItemSettings<TestKey> _settings;
        private readonly TestKey _key;

        private Mock<IStorage<TestResource>> _availableObjectsStorageMock;
        private Mock<IPoolObjectActions<TestResource>> _objectActionsMock;
        private Mock<IObjectUtilizer<TestKey, TestResource>> _objectUtilizerMock;

        public PoolItemTests()
        {
            _key = new TestKey
            {
                Identifier = 846853,
            };
            _settings = new PoolItemSettings<TestKey>
            {
                Key = _key,
                MarkObtainedObjectAsNotAvailable = true,
                MaxObjectsCount = 100,
                ThrowIfCantCreateNewBecauseOfReachedLimit = false,
            };
        }

        [SetUp]
        public void SetUp()
        {
            _availableObjectsStorageMock = Mocks.Storage.GetNew();
            _objectActionsMock = Mocks.ObjectActions.GetNewSuccessful();
            _objectUtilizerMock = Mocks.ObjectUtilizer.GetNew();

            _outPoolObject = null;
            _poolItem = new PoolItem<TestKey, TestResource>(_settings, _availableObjectsStorageMock.Object,
                                                                       _objectActionsMock.Object,
                                                                       _objectUtilizerMock.Object);
        }

        [Test]
        public void AvailableObjectExisted_ObtainReturnsIt()
        {
            AddAvailableObject("1");

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("1"));
        }

        [Test]
        public void NeitherCreateDelegateNorAvailableObject_ObtainReturnsDefault()
        {
            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void MarkingIsOnAndObjectWasObtained_ObtainReturnsDefault()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _outPoolObject, null);

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void MarkingIsOffAndObjectWasObtained_ObtainReturnsIt()
        {
            _settings.MarkObtainedObjectAsNotAvailable = false;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("1"));
            Assert.That(_outPoolObject, Is.EqualTo(_anotherOutPoolObject));
        }

        [Test]
        public void MarkingIsOnAndObjectWasObtainedAndUnmarked_ObtainReturnsIt()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);
            _poolItem.Release(_anotherOutPoolObject);

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("1"));
            Assert.That(_outPoolObject, Is.EqualTo(_anotherOutPoolObject));
        }

        [Test]
        public void MarkingIsOnAndOneAvailableObjectWasObtained_ObtainReturnsAnotherAvailableObject()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            AddAvailableObject("2");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject, Is.Not.EqualTo(_anotherOutPoolObject));
        }

        [Test]
        public void NoAvailableObjectAndMaxCountWasReached_ObtainReturnsDefault()
        {
            _settings.MaxObjectsCount = 2;
            _settings.MarkObtainedObjectAsNotAvailable = true;
            _settings.ThrowIfCantCreateNewBecauseOfReachedLimit = false;
            AddAvailableObject("1");
            AddAvailableObject("2");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);
            _poolItem.TryObtain(out _anotherOutPoolObject, null);

            var getResult = _poolItem.TryObtain(out _outPoolObject, key => new TestResource("won't be used"));

            Assert.That(getResult, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void MaxCountWasReachedButSomeObjectIsAvailable_ObtainReturnsIt()
        {
            _settings.MaxObjectsCount = 2;
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            var available = AddAvailableObject("2");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);

            var getResult = _poolItem.TryObtain(out _outPoolObject, key => new TestResource("won't be used"));

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject, Is.EqualTo(available));
        }

        [Test]
        public void MaxCountWasReachedAndSettingsOrderToThrow_ObtainThrowsDetailedException()
        {
            _settings.MaxObjectsCount = 2;
            _settings.MarkObtainedObjectAsNotAvailable = true;
            _settings.ThrowIfCantCreateNewBecauseOfReachedLimit = true;
            AddAvailableObject("1");
            AddAvailableObject("2");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);
            _poolItem.TryObtain(out _anotherOutPoolObject, null);

            var maxCountReachedException = Assert.Throws<ObjectsMaxCountReachedException<TestKey>>(
                () => _poolItem.TryObtain(out _outPoolObject, key => new TestResource("won't be used")));

            var expectedMessage = string.Format("Object with key='{0}' wasn't created because " +
                                                "max objects count 2 is already reached", _key);
            Assert.That(maxCountReachedException.Message, Is.EqualTo(expectedMessage));
            Assert.That(maxCountReachedException.Key, Is.EqualTo(_key));
            Assert.That(maxCountReachedException.MaxObjectsCount, Is.EqualTo(2));
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void PoolItemIsEmpty_ObtainWithCreateDelegateReturnsNewlyCreatedObject()
        {
            var createDelegateWithCounter = new CreateDelegateWithCounter(3);

            var getResult = _poolItem.TryObtain(out _outPoolObject, createDelegateWithCounter.Delegate);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("3"));
            Assert.That(createDelegateWithCounter.NumberOfCallings, Is.EqualTo(1));
        }

        [Test]
        public void AllObjectsAreNotAvailable_ObtainWithCreateDelegateReturnsNewlyCreatedObject()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _anotherOutPoolObject, null);
            var createDelegateWithCounter = new CreateDelegateWithCounter(3);

            var getResult = _poolItem.TryObtain(out _outPoolObject, createDelegateWithCounter.Delegate);

            Assert.That(getResult, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("3"));
            Assert.That(createDelegateWithCounter.NumberOfCallings, Is.EqualTo(1));
        }

        [Test]
        public void MarkingIsOff_ReferenceToCreatedObjectIsPreserved()
        {
            _settings.MarkObtainedObjectAsNotAvailable = false;
            var createDelegateWithCounter = new CreateDelegateWithCounter(1);

            _poolItem.TryObtain(out _outPoolObject, createDelegateWithCounter.Delegate);

            Assert.That(_poolItem.AvailableObjects.Count, Is.EqualTo(1));
        }

        [Test]
        public void MarkingIsOn_ReferenceToCreatedObjectIsPreserved()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            var createDelegateWithCounter = new CreateDelegateWithCounter(1);

            _poolItem.TryObtain(out _outPoolObject, createDelegateWithCounter.Delegate);

            Assert.That(_poolItem.NotAvailableObjectsCount, Is.EqualTo(1));
        }

        [Test]
        public void MarkingIsOffAndUnmarkingOfStrangeObject_DetailedInvalidOperationExceptionIsThrown()
        {
            _settings.MarkObtainedObjectAsNotAvailable = false;
            var unknownObject = new TestResource("asd");

            var markingIsOffException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _poolItem.Release(unknownObject));

            Assert.That(markingIsOffException.Key, Is.EqualTo(_key));
            Assert.That(markingIsOffException.Object, Is.EqualTo(unknownObject));
            Assert.That(markingIsOffException.Message, Is.EqualTo("Operation of marking object as available is invalid " +
                                                                  "because marking was ordered to be off"));
        }

        [Test]
        public void MarkingIsOffAndUnmarkingOfAvailableObject_DetailedInvalidPoolOperationExceptionIsThrown()
        {
            _settings.MarkObtainedObjectAsNotAvailable = false;
            var availableObject = AddAvailableObject("4");

            var markingIsOffException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _poolItem.Release(availableObject));

            Assert.That(markingIsOffException.Key, Is.EqualTo(_key));
            Assert.That(markingIsOffException.Object, Is.EqualTo(availableObject));
            Assert.That(markingIsOffException.Message, Is.EqualTo("Operation of marking object as available is invalid " +
                                                                  "because marking was ordered to be off"));
        }

        [Test]
        public void MarkingIsOnAndUnmarkingOfStrangeObject_DetailedInvalidOperationExceptionIsThrown()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            var unknownObject = new TestResource("asd");

            var strangeObjectException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _poolItem.Release(unknownObject));
            
            Assert.That(strangeObjectException.Key, Is.EqualTo(_key));
            Assert.That(strangeObjectException.Object, Is.EqualTo(unknownObject));
            Assert.That(strangeObjectException.Message, Is.EqualTo("Marking object as available has been declined because " +
                                                                   "this object wasn't created by pool, it's a stranger"));
        }

        [Test]
        public void MarkingIsOnAndUnmarkingOfAvailableObject_DetailedInvalidOperationExceptionIsThrown()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            var availableObject = AddAvailableObject("2");

            var unmarkOperationException = Assert.Throws<InvalidPoolOperationException<TestKey, TestResource>>(
                () => _poolItem.Release(availableObject));

            Assert.That(unmarkOperationException.Key, Is.EqualTo(_key));
            Assert.That(unmarkOperationException.Object, Is.EqualTo(availableObject));
            Assert.That(unmarkOperationException.Message, Is.EqualTo("Marking object as available has been declined " +
                                                                     "because it's currently available. Object should " +
                                                                     "be marked as not available first"));
        }
        
        [Test]
        public void AllAvailableObjectsBecameInvalid_PoolItemForgotsThemAndReturnsAnotherObject()
        {
            AddAvailableObject("1" + Mocks.ObjectActions.SubstringOfInvalidObject);
            AddAvailableObject("2" + Mocks.ObjectActions.SubstringOfInvalidObject);
            var createDelegateWithCounter = new CreateDelegateWithCounter(5);

            var getStatus = _poolItem.TryObtain(out _outPoolObject, createDelegateWithCounter.Delegate);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("5"));
            Assert.That(createDelegateWithCounter.NumberOfCallings, Is.EqualTo(1));
            Assert.That(_poolItem.AllObjectsCount, Is.EqualTo(1));
        }

        [Test]
        public void AvailableObjectBecameInvalid_ObtainCallsObjectUtilizerForThemAndKeepsOthers()
        {
            var invalidAvailable1 = AddAvailableObject("1" + Mocks.ObjectActions.SubstringOfInvalidObject);
            var invalidAvailable2 = AddAvailableObject("2" + Mocks.ObjectActions.SubstringOfInvalidObject);
            AddAvailableObject("3");

            _poolItem.TryObtain(out _outPoolObject, null);

            _objectUtilizerMock.Verify(x => x.Utilize(_key, invalidAvailable1, _poolItem), Times.Once);
            _objectUtilizerMock.Verify(x => x.Utilize(_key, invalidAvailable2, _poolItem), Times.Once);
            _objectUtilizerMock.Verify(x => x.Utilize(_key, It.IsAny<TestResource>(), It.IsAny<object>()), Times.Exactly(2));
        }

        [Test]
        public void AvailableObjectBecameInvalid_ObtainDisposesThemAndDoesNotAffectOthers()
        {
            var invalidAvailable1 = AddAvailableObject("1" + Mocks.ObjectActions.SubstringOfInvalidObject);
            var invalidAvailable2 = AddAvailableObject("2" + Mocks.ObjectActions.SubstringOfInvalidObject);
            AddAvailableObject("3");

            _poolItem.TryObtain(out _outPoolObject, null);

            _objectActionsMock.Verify(x => x.Dispose(invalidAvailable1), Times.Once);
            _objectActionsMock.Verify(x => x.Dispose(invalidAvailable2), Times.Once);
            _objectActionsMock.Verify(x => x.Dispose(It.IsAny<TestResource>()), Times.Exactly(2));
        }

        [Test]
        public void MarkingIsOnAndUnmarkingOfObtainedInvalidObject_ItIsForgottenSilently()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _outPoolObject, null);

            _outPoolObject.Value += Mocks.ObjectActions.SubstringOfInvalidObject;
            _poolItem.Release(_outPoolObject);

            Assert.That(_poolItem.AllObjectsCount, Is.EqualTo(0));
        }

        [Test]
        public void MarkingIsOnAndUnmarkingOfObtainedInvalidObject_ObjectUtilizerIsCalled()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            AddAvailableObject("1");
            _poolItem.TryObtain(out _outPoolObject, null);

            _outPoolObject.Value += Mocks.ObjectActions.SubstringOfInvalidObject;
            _poolItem.Release(_outPoolObject);

            _objectUtilizerMock.Verify(x => x.Utilize(_key, _outPoolObject, _poolItem), Times.Once);
            _objectUtilizerMock.Verify(x => x.Utilize(_key, It.IsAny<TestResource>(), It.IsAny<object>()), Times.Once);
        }

        [Test]
        public void MarkingIsOnAndUnmarkingOfObtainedInvalidObject_ObjectIsDisposed()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            var invalidInFutureResource = AddAvailableObject("1");
            _poolItem.TryObtain(out _outPoolObject, null);

            _outPoolObject.Value += Mocks.ObjectActions.SubstringOfInvalidObject;
            _poolItem.Release(_outPoolObject);

            _objectActionsMock.Verify(x => x.Dispose(invalidInFutureResource));
        }

        [Test]
        public void CreateDelegateThrewException_DetailedObjectCreationFailedExceptionIsThrown()
        {
            Func<TestKey,TestResource> throwingCreateDelegate = key => { throw new NetworkInformationException(); };

            var creationFailedException = Assert.Throws<ObjectCreationFailedException<TestKey,TestResource>>(
                () => _poolItem.TryObtain(out _outPoolObject, throwingCreateDelegate));

            var expectedMessage = string.Format("Creation object with key='{0}' failed. " +
                                                "Look at inner exception for details", _key);
            Assert.That(creationFailedException.Message, Is.EqualTo(expectedMessage));
            Assert.That(creationFailedException.InnerException, Is.TypeOf<NetworkInformationException>());
            Assert.That(creationFailedException.Key, Is.EqualTo(_key));
            Assert.That(creationFailedException.UsedCreateDelegate, Is.EqualTo(throwingCreateDelegate));
        }

        [Test]
        public void CreateDelegateProducedInvalidObject_DetailedObjectCreationFailedExceptionIsThrown()
        {
            Func<TestKey, TestResource> createInvalidObjectDelegate =
                key => new TestResource(Mocks.ObjectActions.SubstringOfInvalidObject);

            var invalidObjectCreatedException = Assert.Throws<ObjectCreationFailedException<TestKey, TestResource>>(
                () => _poolItem.TryObtain(out _outPoolObject, createInvalidObjectDelegate));

            var expectedMessage = string.Format("Provided delegate created invalid object with key='{0}'", _key);
            Assert.That(invalidObjectCreatedException.Message, Is.EqualTo(expectedMessage));
            Assert.That(invalidObjectCreatedException.InnerException, Is.Null);
            Assert.That(invalidObjectCreatedException.Key, Is.EqualTo(_key));
            Assert.That(invalidObjectCreatedException.UsedCreateDelegate, Is.EqualTo(createInvalidObjectDelegate));
        }

        [Test]
        public void CreateDelegateProducedInvalidObject_UtilizerIsNotCalled()
        {
            Func<TestKey, TestResource> createInvalidObjectDelegate =
                key => new TestResource(Mocks.ObjectActions.SubstringOfInvalidObject);

            try
            {
                _poolItem.TryObtain(out _outPoolObject, createInvalidObjectDelegate);
            }
            catch (Exception) {}

            _objectUtilizerMock.Verify(x => x.Utilize(_key, createInvalidObjectDelegate(_key), _poolItem), Times.Never);
            _objectUtilizerMock.Verify(x => x.Utilize(_key, It.IsAny<TestResource>(), It.IsAny<object>()), Times.Never);
        }

        [Test]
        public void CreateDelegateProducedInvalidObject_ObjectIsDisposed()
        {
            Func<TestKey, TestResource> createInvalidObjectDelegate =
                key => new TestResource(Mocks.ObjectActions.SubstringOfInvalidObject);

            try
            {
                _poolItem.TryObtain(out _outPoolObject, createInvalidObjectDelegate);
            }
            catch (Exception) { }

            _objectActionsMock.Verify(x => x.Dispose(It.IsAny<TestResource>()), Times.Once);
        }

        [Test]
        public void NoCreateDelegateAndAllAvailableObjectsWereInvalid_ObtainReturnsDefault()
        {
            AddAvailableObject("1" + Mocks.ObjectActions.SubstringOfInvalidObject);
            AddAvailableObject("2" + Mocks.ObjectActions.SubstringOfInvalidObject);

            var getResult = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getResult, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void AllRequiredDependenciesAreSatisfied()
        {
            CreatePoolItemWithoutDependencyAndCheck(null, _availableObjectsStorageMock.Object,
                                                    _objectActionsMock.Object, _objectUtilizerMock.Object,
                                                    "settings");

            CreatePoolItemWithoutDependencyAndCheck(_settings, null, _objectActionsMock.Object,
                                                    _objectUtilizerMock.Object, "availableObjectsStorage");

            CreatePoolItemWithoutDependencyAndCheck(_settings, _availableObjectsStorageMock.Object, null,
                                                    _objectUtilizerMock.Object, "objectActions");

            CreatePoolItemWithoutDependencyAndCheck(_settings, _availableObjectsStorageMock.Object, _objectActionsMock.Object,
                                                    null, "objectUtilizer");

            TestDelegate constructWithoutKey = () => new PoolItem<TestKey, TestResource>(new PoolItemSettings<TestKey>(),
                                                                                                  _availableObjectsStorageMock.Object,
                                                                                                  _objectActionsMock.Object,
                                                                                                  _objectUtilizerMock.Object);
            var noKeyException = Assert.Throws<ArgumentException>(constructWithoutKey);
            Assert.That(noKeyException.ParamName, Is.EqualTo("settings.Key"));
            Assert.That(noKeyException.Message, Is.StringContaining("PoolItemSettings should contain key"));
        }

        [Test]
        public void AvailableObjectWasSaidToBeKilled_ObtainDoesNotReturnIt()
        {
            var availableObject = AddAvailableObject("toKill");

            _poolItem.MarkObjectForKilling(availableObject);
            var getStatus = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getStatus, Is.False);
            Assert.That(_outPoolObject, Is.Null);
        }

        [Test]
        public void AvailableObjectWasSaidToBeKilled_ObtainRemovesItAndDisposesIt()
        {
            var availableObject = AddAvailableObject("toKill");

            _poolItem.MarkObjectForKilling(availableObject);
            var getStatus = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(_poolItem.AvailableObjects.Count, Is.EqualTo(0));
            _objectActionsMock.Verify(x => x.Dispose(availableObject));
        }

        [Test]
        public void AvailableObjectWasSaidToBeKilled_ObtainCallsUtilizer()
        {
            var availableObject = AddAvailableObject("toKill");

            _poolItem.MarkObjectForKilling(availableObject);
            _poolItem.TryObtain(out _outPoolObject, null);

            _objectUtilizerMock.Verify(x => x.Utilize(_key,availableObject,_poolItem));
        }

        [Test]
        public void AvailableObjectWasSaidToBeKilled_ObtainReturnsAnotherAvailableObject()
        {
            var availableObject = AddAvailableObject("toKill");
            AddAvailableObject("toReturn");

            _poolItem.MarkObjectForKilling(availableObject);
            var getStatus = _poolItem.TryObtain(out _outPoolObject, null);

            Assert.That(getStatus, Is.True);
            Assert.That(_outPoolObject.Value, Is.EqualTo("toReturn"));
        }

        [Test]
        public void UnmarkingOfObjectSaidToBeKilled_ItIsRemovedAndDisposed()
        {
            var poolObject = AddAvailableObject("object");
            _poolItem.TryObtain(out _outPoolObject, null);

            _poolItem.MarkObjectForKilling(poolObject);
            _poolItem.Release(poolObject);

            Assert.That(_poolItem.AvailableObjects.Count, Is.EqualTo(0));
            _objectActionsMock.Verify(x => x.Dispose(poolObject));
        }

        [Test]
        public void UnmarkingOfObjectSaidToBeKilled_UtilizerIsCalled()
        {
            var poolObject = AddAvailableObject("object");
            _poolItem.TryObtain(out _outPoolObject, null);

            _poolItem.MarkObjectForKilling(poolObject);
            _poolItem.Release(poolObject);

            _objectUtilizerMock.Verify(x => x.Utilize(_key, poolObject, _poolItem));
        }

        [Test]
        public void KillingOfUnknownObject_ItemDoesNotThrowAndIgnoresSilently()
        {
            var unknownObject = new TestResource("stranger");

            Assert.DoesNotThrow(() => _poolItem.MarkObjectForKilling(unknownObject));
        }

        [Test]
        public void DisposeWasCalled_AllObjectsAreDisposedAndUtilizerIsCalled()
        {
            _settings.MarkObtainedObjectAsNotAvailable = true;
            var object1 = AddAvailableObject("1");
            var object2 = AddAvailableObject("2");
            var object3 = AddAvailableObject("3");
            var object4 = AddAvailableObject("4");
            _poolItem.TryObtain(out _outPoolObject, null);
            _poolItem.TryObtain(out _outPoolObject, null);

            _poolItem.Dispose();

            Action<TestResource> verifyDisposingAndUtilizing = resource =>
            {
                _objectActionsMock.Verify(x => x.Dispose(resource), Times.Once);
                _objectUtilizerMock.Verify(x => x.Utilize(_key, resource, _poolItem), Times.Once);
            };
            verifyDisposingAndUtilizing(object1);
            verifyDisposingAndUtilizing(object2);
            verifyDisposingAndUtilizing(object3);
            verifyDisposingAndUtilizing(object4);
        }

        private void CreatePoolItemWithoutDependencyAndCheck(PoolItemSettings<TestKey> settings,
                                                             IStorage<TestResource> availableObjectsStorage,
                                                             IPoolObjectActions<TestResource> objectActions,
                                                             IObjectUtilizer<TestKey, TestResource> objectUtilizer,
                                                             string paramNameInException)
        {
            TestDelegate createPoolItem = () => new PoolItem<TestKey, TestResource>(settings,
                                                                                    availableObjectsStorage,
                                                                                    objectActions,
                                                                                    objectUtilizer);

            var noDependencyException = Assert.Throws<ArgumentNullException>(createPoolItem);

            Assert.That(noDependencyException.ParamName, Is.EqualTo(paramNameInException));
        }

        private TestResource AddAvailableObject(string value)
        {
            var resource = new TestResource(value);
            _availableObjectsStorageMock.Object.Add(resource);
            return resource;
        }
    }
}
