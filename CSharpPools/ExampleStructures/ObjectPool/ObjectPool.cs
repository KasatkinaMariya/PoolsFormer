using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.ObjectPool
{
    public class ObjectPool<TK,TV> : IDisposable
    {
        private readonly ObjectPoolSettings<TK,TV> _settings;
        private readonly PoolController<TK, TV> _poolController;

        public ObjectPool(ObjectPoolSettings<TK,TV> settings, IPoolObjectActions<TV> objectActions)
        {
            _settings = settings;
            _poolController = CreateObjectPoolControllerInstance(objectActions);
        }

        public TV Obtain(TK key, Func<TK, TV> createDelegate = null)
        {
            var noObjectDirection = new DirectionIfNoObjectIsAvailable<TK, TV>
            {
                CreateDelegateIfNoObjectIsAvailable = createDelegate,
            };
            return Obtain(key, noObjectDirection);
        }

        public TV Obtain(TK key, DirectionIfNoObjectIsAvailable<TK, TV> noObjectDirection)
        {
            if (noObjectDirection.CreateDelegateIfNoObjectIsAvailable == null)
                noObjectDirection.CreateDelegateIfNoObjectIsAvailable = _settings.DefaultCreateDelegate;

            TV outObject;
            _poolController.Obtain(key, out outObject, noObjectDirection);
            return outObject;
        }

        public void Release(TV poolObject)
        {
            _poolController.Release(poolObject);
        }

        public void Dispose()
        {
            _poolController.Dispose();
        }

        private PoolController<TK, TV> CreateObjectPoolControllerInstance(IPoolObjectActions<TV> objectActions)
        {
            var objectUtilizer = new ObjectUtilizer<TK, TV>();

            var basicPoolSettings = new PoolItemsStorageSettings
            {
                AllowOnlyOneUserPerObject = true,
                BalancingStrategy = _settings.BalancingStrategy,
                MaxObjectsCountPerKey = _settings.MaxObjectCountPerKey,
                ThrowIfCantCreateObjectBecauseOfReachedLimit = _settings.ThrowIfCantCreateBecauseOfReachedLimit,
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

            var singleUsePool = new PWSingleUseEnforcingWrapper<TK, TV>(stateMonitoringPool);

            var objectResettingPool = new PWObjectResettingWrapper<TK, TV>(singleUsePool,
                                                                           objectActions,
                                                                           objectUtilizer);

            var poolControllerSettings = new PoolControllerSettings
            {
                CallingReleaseOperationWillHappen = true,
            };
            return new PoolController<TK, TV>(poolControllerSettings, objectResettingPool);
        }
    }
}
