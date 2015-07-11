using System;
using System.Collections.Concurrent;
using System.Threading;
using PoolsLibrary.Pool;

namespace PoolsLibrary.Controller
{
    public class PoolController<TK, TV> : IDisposable
    {
        internal ConcurrentDictionary<TV, TK> ObtainedObjectToItsKey
        {
            get { return _obtainedObjectToItsKey; }
        }

        private readonly PoolControllerSettings _settings;
        private readonly ConcurrentDictionary<TV, TK> _obtainedObjectToItsKey = new ConcurrentDictionary<TV, TK>();
        private bool _disposingWasCalled = false;

        private readonly IPool<TK, TV> _pool;

        public PoolController(PoolControllerSettings settings, IPool<TK, TV> pool)
        {
            _settings = settings;
            _pool = pool;
        }

        public bool Obtain(TK key, out TV outPoolObject, DirectionIfNoObjectIsAvailable<TK, TV> noObjectDirection)
        {
            if (_disposingWasCalled)
            {
                outPoolObject = default(TV);
                return false;
            }

            var adaptedDirection = AdaptDirection(noObjectDirection);

            int curAttemptNumber = 0;
            while (++curAttemptNumber <= adaptedDirection.AttemptsNumber)
            {
                if (curAttemptNumber > 1)
                    Thread.Sleep(adaptedDirection.OneIntervalBetweenAttemptsInSeconds * 1000);

                try
                {
                    var curAttemptDelegate = ChooseCreateDelegate(adaptedDirection, curAttemptNumber);
                    SharedEnvironment.Log.DebugFormat("Attempt #{0} with delegate='{1}'", curAttemptNumber, curAttemptDelegate);

                    if (_pool.TryObtain(key, out outPoolObject, curAttemptDelegate))
                    {
                        if (_settings.CallingReleaseOperationWillHappen)
                            ObtainedObjectToItsKey.TryAdd(outPoolObject, key);
                        return true;
                    }
                }
                catch (Exception e)
                {
                    var internalErrorMessage = string.Format("Something failed during attempt #{0} of obtaining object " +
                                                             "with key='{1}'. Look at inner exception for details",
                                                             curAttemptNumber, key);
                    throw new PoolException<TK>(key, internalErrorMessage, e);
                }
            }

            outPoolObject = default(TV);
            return false;
        }

        public void Release(TV objectToRelease)
        {
            if (_disposingWasCalled)
                return;
            CheckPromiseToCallReleaseOperation(objectToRelease);

            TK key;
            if (ObtainedObjectToItsKey.TryRemove(objectToRelease, out key))
            {
                _pool.Release(key, objectToRelease);
                return;
            }

            var notObtainedObjectMessage = "Only obtained objects are allowed to be released";
            throw new InvalidPoolOperationException<TK, TV>(objectToRelease, message:notObtainedObjectMessage);
        }

        /// For performance reasons there is no synchronization between disposing and
        /// obtaining+releasing operatons in the internal pool components. So user is
        /// supposed to dispose controller only after completion of all other operations.
        /// Otherwise some resources may be not disposed or even pool consistency may be broken.
        /// PoolController helps an user a little bit: if some operation is called after
        /// starting disposing, it will be rejected (silently ignored).
        /// <summary>
        /// May be called only after completion of all obtaining and releasing operations.
        /// </summary>
        public void Dispose()
        {
            SharedEnvironment.Log.Debug("Disposing has started");
            _disposingWasCalled = true;

            _pool.Dispose();
            SharedEnvironment.Log.Debug("Disposing has finished");
        }

        private DirectionIfNoObjectIsAvailable<TK, TV> AdaptDirection
                                                (DirectionIfNoObjectIsAvailable<TK, TV> originalDirection)
        {
            var toReturn = originalDirection ?? DirectionIfNoObjectIsAvailable<TK, TV>.DoNotWaitDirection;

            if (!_settings.CallingReleaseOperationWillHappen)
                toReturn.AttemptsNumber = 1;

            return toReturn;
        }

        private Func<TK, TV> ChooseCreateDelegate(DirectionIfNoObjectIsAvailable<TK, TV> direction,
                                          int curAttemptNumber)
        {
            var isLastAttempt = curAttemptNumber == direction.AttemptsNumber;
            return isLastAttempt
                   ? direction.CreateDelegateIfNoObjectIsAvailable
                   : null;
        }

        private void CheckPromiseToCallReleaseOperation(TV objectToRelease)
        {
            if (_settings.CallingReleaseOperationWillHappen)
                return;

            var noPromiseMessage = "In order to release objects promise it by setting " +
                                   "PoolControllerSettings.CallingReleaseOperationWillHappen " +
                                   "to true. Currently it's false";
            throw new InvalidPoolOperationException<TK, TV>(objectToRelease, message:noPromiseMessage );
        }
    }
}
