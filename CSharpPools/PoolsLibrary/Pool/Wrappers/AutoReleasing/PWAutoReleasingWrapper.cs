using System;
using System.Collections.Concurrent;
using PoolsLibrary.Controller;
using PoolsLibrary.Pool.BasicFunctionality.Item;

namespace PoolsLibrary.Pool.Wrappers.AutoReleasing
{
    public class PWAutoReleasingWrapper<TK,TV> : IInternalPool<TK,TV>
        where TV : ISelfSufficientObject<TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> IInternalPool<TK, TV>.KeyToPoolItem
        {
            get { return _basePool.KeyToPoolItem; }
        }

        private readonly IInternalPool<TK, TV> _basePool;
        private PoolController<TK, TV> _poolController;

        public PWAutoReleasingWrapper(IPool<TK, TV> basePool)
        {
            _basePool = basePool as IInternalPool<TK,TV>;
        }

        public bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            CheckPoolController(key, default(TV));

            var getStatus = _basePool.TryObtain(key, out outPoolObject, createDelegateIfNoObjectIsAvailable);
            if (getStatus)
                outPoolObject.ReadyToBeReleased += CallTakingBackLikeUser;
            return getStatus;
        }

        public void Release(TK key, TV objectToRelease)
        {
            CheckPoolController(key,objectToRelease);

            objectToRelease.ReadyToBeReleased -= CallTakingBackLikeUser;
            _basePool.Release(key, objectToRelease);
        }

        public void Dispose()
        {
            _basePool.Dispose();
        }

        public void SetPoolController(PoolController<TK, TV> poolController)
        {
            _poolController = poolController;
        }

        private void CallTakingBackLikeUser(object sender, ReadyToBeReleasedEventArgs<TV> readyToBeReleasedEventArgs)
        {
            _poolController.Release(readyToBeReleasedEventArgs.PoolObject);
        }

        private void CheckPoolController(TK key, TV poolObject)
        {
            if (_poolController != null)
                return;

            var noControllerMessage = "PWAutoReleasingWrapper needs specified instance of PoolController. " +
                                      "Call SetPoolController(..) before starting usage of pool";
            throw new InvalidPoolOperationException<TK, TV>(poolObject, key, noControllerMessage);
        }
    }
}
