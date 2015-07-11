using System;
using System.Collections.Generic;
using System.Net;
using Moq;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectActions.Notification;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool;
using PoolsLibrary.Pool.BasicFunctionality.Item;
using PoolsLibrary.Pool.BasicFunctionality.Storages;
using UnitTests.TestEntities;
using UnitTests.Utils;

namespace UnitTests
{
    static class Mocks
    {
        internal static class Pool
        {
            public static string ResourceValueAfterWaitings = "success";

            public static Mock<IInternalPool<TestKey, TestResource>> GetNewReturningDifferentObjects()
            {
                int counter = 1;
                Func<TestKey, TestResource> createResourceByKey =
                    key => new TestResource(string.Format("{0} {1}", key.Identifier, counter++));

                TestResource successOutResource;
                var successPoolMock = new Mock<IInternalPool<TestKey, TestResource>>(MockBehavior.Strict);
                successPoolMock.Setup(x => x.TryObtain(It.IsAny<TestKey>(), out successOutResource, It.IsAny<Func<TestKey, TestResource>>()))
                               .OutCallback((TestKey key, out TestResource outResource, Func<TestKey, TestResource> createDelegate)
                                    => outResource = createResourceByKey(key))
                               .Returns<TestKey, TestResource, Func<TestKey, TestResource>>((key, outResource, createDelegate)
                                    => true);
                successPoolMock.Setup(x => x.Release(It.IsAny<TestKey>(), It.IsAny<TestResource>()));

                return successPoolMock;
            }

            public static Mock<IInternalPool<TestKey, TestResource>> GetNewReturningSameObject()
            {
                var sameOutResource = new TestResource("gfd");
                var sameObjectPoolMock = new Mock<IInternalPool<TestKey, TestResource>>(MockBehavior.Strict);
                sameObjectPoolMock.Setup(x => x.TryObtain(It.IsAny<TestKey>(), out sameOutResource, It.IsAny<Func<TestKey, TestResource>>()))
                               //.OutCallback((TestKey key, out TestResource outResource, Func<TestKey, TestResource> createDelegate)
                               //     => outResource = createResourceByKey(key))
                                  .Returns<TestKey, TestResource, Func<TestKey, TestResource>>((key, outResource, createDelegate)
                                    => true);

                return sameObjectPoolMock;
            }

            public static Mock<IInternalPool<TestKey, TestResource>> GetNewReturningNoObject()
            {
                TestResource noOutResource;
                var noObjectPoolMock = new Mock<IInternalPool<TestKey, TestResource>>(MockBehavior.Strict);
                noObjectPoolMock.Setup(x => x.TryObtain(It.IsAny<TestKey>(), out noOutResource, It.IsAny<Func<TestKey, TestResource>>()))
                                .OutCallback((TestKey key, out TestResource outResource, Func<TestKey, TestResource> createDelegate)
                                     => outResource = null)
                                .Returns(false);

                return noObjectPoolMock;
            }

            public static Mock<IInternalPool<TestKey, TestResource>> GetNewReturningNoObjectThreeTimes()
            {
                TestResource threeTimesNoOutResource;
                int curCallNumber = 0;
                var noObjectThreeTimesPoolMock = new Mock<IInternalPool<TestKey, TestResource>>(MockBehavior.Strict);
                noObjectThreeTimesPoolMock.Setup(x => x.TryObtain(It.IsAny<TestKey>(), out threeTimesNoOutResource, It.IsAny<Func<TestKey, TestResource>>()))
                                          .OutCallback((TestKey key, out TestResource outResource, Func<TestKey, TestResource> createDelegate)
                                               => outResource = (++curCallNumber == 4) ? new TestResource(ResourceValueAfterWaitings) : null)
                                          .Returns<TestKey, TestResource, Func<TestKey, TestResource>>((key, outResource, createDelegate)
                                               => curCallNumber == 4);

                return noObjectThreeTimesPoolMock;
            }

            public static Mock<IInternalPool<TestKey, TestResource>> GetNewThrowing()
            {
                TestResource throwedOutResource;
                var throwingPoolMock = new Mock<IInternalPool<TestKey, TestResource>>(MockBehavior.Strict);
                throwingPoolMock.Setup(x => x.TryObtain(It.IsAny<TestKey>(), out throwedOutResource, It.IsAny<Func<TestKey, TestResource>>()))
                                .Throws<ObjectCreationFailedException<TestKey, TestResource>>();
                throwingPoolMock.Setup(x => x.Release(It.IsAny<TestKey>(), It.IsAny<TestResource>()))
                                .Throws<InvalidPoolOperationException<TestKey, TestResource>>();

                return throwingPoolMock;
            }
        }

        internal static class ObjectActions
        {
            public static string SubstringOfInvalidObject = "invalid";

            public static Mock<IPoolObjectActions<TestResource>> GetNewSuccessful()
            {
                var objectActionsMock = new Mock<IPoolObjectActions<TestResource>>(MockBehavior.Strict);
                objectActionsMock.Setup(x => x.IsValid(It.IsAny<TestResource>()))
                                 .Returns<TestResource>(r => !r.Value.Contains(SubstringOfInvalidObject));
                objectActionsMock.Setup(x => x.Ping(It.IsAny<TestResource>()))
                                 .Returns(true);
                objectActionsMock.Setup(x => x.Dispose(It.IsAny<TestResource>()));

                return objectActionsMock;
            }

            public static Mock<IPoolObjectActions<TestResource>> GetNewFailingOnPing()
            {
                var throwingActionsMock = new Mock<IPoolObjectActions<TestResource>>(MockBehavior.Strict);

                throwingActionsMock.Setup(x => x.Ping(It.IsAny<TestResource>()))
                                   .Returns(false);

                throwingActionsMock.Setup(x => x.IsValid(It.IsAny<TestResource>()))
                                   .Returns(true);

                return throwingActionsMock;
            }
        }

        internal static class ObjectUtilizer
        {
            public static Mock<IObjectUtilizer<TestKey, TestResource>> GetNew()
            {
                var objectUtilizerMock = new Mock<IObjectUtilizer<TestKey, TestResource>>(MockBehavior.Strict);
                objectUtilizerMock.Setup(x => x.Utilize(It.IsAny<TestKey>(), It.IsAny<TestResource>(), It.IsAny<object>()));

                return objectUtilizerMock;
            }
        }

        internal static class Storage
        {
            public static Mock<IStorage<TestResource>> GetNew()
            {
                var storage = new List<TestResource>();
                var storageMock = new Mock<IStorage<TestResource>>(MockBehavior.Strict);
                storageMock.Setup(x => x.Add(It.IsAny<TestResource>()))
                           .Callback((TestResource r) => storage.Add(r));
                storageMock.Setup(x => x.Remove())
                           .Returns(() => storage[0])
                           .Callback(() => storage.RemoveAt(0));
                storageMock.Setup(x => x.Contains(It.IsAny<TestResource>()))
                           .Returns<TestResource>(storage.Contains);
                storageMock.Setup(x => x.Count)
                           .Returns(() => storage.Count);

                return storageMock;
            }
        }

        internal static class Notifier
        {
            public static Mock<INotifier> GetNew()
            {
                var notifierMock = new Mock<INotifier>(MockBehavior.Strict);
                notifierMock.Setup(x => x.Notify(It.IsAny<UserDefinedActionError<TestResource>>()));
                return notifierMock;
            }
        }
    }
}