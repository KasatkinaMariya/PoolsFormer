using System;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.Cache
{
    public class Cache<TK,TV> : IDisposable
    {
        private readonly CacheSettings<TK, TV> _settings;
        private readonly PoolController<TK, TV> _poolController;

        public Cache(CacheSettings<TK, TV> settings, IPoolObjectActions<TV> objectActions)
        {
            _settings = settings;
            _poolController = CreateCacheControllerInstance(objectActions);
        }

        public TV Get(TK key, Func<TK,TV> createDelegate = null)
        {
            var noObjectDirection = new DirectionIfNoObjectIsAvailable<TK, TV>
            {
                CreateDelegateIfNoObjectIsAvailable = createDelegate ?? _settings.DefaultCreateDelegate,
            };

            TV outObject;
            _poolController.Obtain(key, out outObject, noObjectDirection);
            return outObject;
        }

        public void Dispose()
        {
            _poolController.Dispose();
        }

        private PoolController<TK, TV> CreateCacheControllerInstance(IPoolObjectActions<TV> objectActions)
        {
            var objectUtilizer = new ObjectUtilizer<TK, TV>();

            var basicPoolSettings = new PoolItemsStorageSettings
            {
                AllowOnlyOneUserPerObject = false,
                BalancingStrategy = LoadBalancingStrategy.DistributedAmongAllObjects,
            };
            var basicPool = new PoolItemsStorage<TK, TV>(basicPoolSettings, objectActions, objectUtilizer);

            var stateMonitoringSettings = new PWObjectStateMonitoringSettings
            {
                MaxObjectIdleTimeSpanInSeconds = _settings.MaxObjectIdleTimeSpanInSeconds,
                MaxObjectLifetimeInSeconds = _settings.MaxObjectIdleTimeSpanInSeconds,
                TimeSpanBetweenRevivalsInSeconds = _settings.MonitorTimeSpanInSeconds,
            };
            var stateMonitoringPool = new PWObjectStateMonitoringWrapper<TK, TV>(stateMonitoringSettings,
                                                                                 basicPool,
                                                                                 objectActions,
                                                                                 objectUtilizer);

            var poolControllerSettings = new PoolControllerSettings
            {
                CallingReleaseOperationWillHappen = false,
            };
            return new PoolController<TK, TV>(poolControllerSettings, stateMonitoringPool);
        }
    }
}
