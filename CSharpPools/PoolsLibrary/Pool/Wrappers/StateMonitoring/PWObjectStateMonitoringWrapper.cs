using System;
using System.Collections.Concurrent;
using System.Linq;
using System.Timers;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality.Item;

namespace PoolsLibrary.Pool.Wrappers.StateMonitoring
{
    public class PWObjectStateMonitoringWrapper<TK, TV> : IInternalPool<TK, TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> IInternalPool<TK, TV>.KeyToPoolItem
        {
            get { return _basePool.KeyToPoolItem; }
        }
        internal ConcurrentDictionary<TV, ObjectLifetimeData<TK>> ObjectToLifetimeData
        {
            get { return _objectToLifetimeData; }
        }

        private readonly PWObjectStateMonitoringSettings _settings;
        private readonly ConcurrentDictionary<TV, ObjectLifetimeData<TK>> _objectToLifetimeData;
        private Timer _cleaningTimer;

        private ObjectLifetimeData<TK> _notInterestingOutData;
        private readonly bool _shouldWatchTimestamps;

        private readonly IInternalPool<TK, TV> _basePool;
        private readonly IPoolObjectActions<TV> _objectActions;
        private readonly IObjectUtilizer<TK, TV> _objectUtilizer;

        public PWObjectStateMonitoringWrapper(PWObjectStateMonitoringSettings settings,
                                IPool<TK, TV> basePool,
                                IPoolObjectActions<TV> objectActions,
                                IObjectUtilizer<TK, TV> objectUtilizer)
        {
            _settings = settings;
            _basePool = basePool as IInternalPool<TK, TV>;
            _objectActions = objectActions;
            _objectUtilizer = objectUtilizer;

            _shouldWatchTimestamps = _settings.MaxObjectIdleTimeSpanInSeconds.HasValue
                                     || _settings.MaxObjectLifetimeInSeconds.HasValue;
            _objectToLifetimeData = new ConcurrentDictionary<TV, ObjectLifetimeData<TK>>();

            _objectUtilizer.ObjectIsGone += OnObjectIsGone;
            InitCleaningTimer();
        }

        public bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            if (!_basePool.TryObtain(key, out outPoolObject, createDelegateIfNoObjectIsAvailable))
                return false;

            RememberUsageTimestamp(key, outPoolObject);
            return true;
        }

        public void Release(TK key, TV objectToRelease)
        {
            if (_shouldWatchTimestamps)
                RememberUsageTimestamp(key, objectToRelease);

            _basePool.Release(key, objectToRelease);
        }

        public void Dispose()
        {
            _cleaningTimer.Stop();
            _objectUtilizer.ObjectIsGone -= OnObjectIsGone;
            _basePool.Dispose();
        }

        internal void DropLifelessObjectsAndWakeupOthers()
        {
            foreach (var item in _objectToLifetimeData)
            {
                if (IsUseful(poolObject: item.Key, lifetimeData: item.Value)
                    && _objectActions.Ping(item.Key))
                    continue;

                _objectToLifetimeData.TryRemove(item.Key, out _notInterestingOutData);
                _objectUtilizer.Utilize(item.Value.Key, item.Key, this);
            }
        }

        private bool IsUseful(TV poolObject, ObjectLifetimeData<TK> lifetimeData)
        {
            if (!_objectActions.IsValid(poolObject))
                return false;

            var isNew = NowIsClose(lifetimeData.CreationTimeStamp, _settings.MaxObjectLifetimeInSeconds);
            var isActive = NowIsClose(lifetimeData.LastUsageTimeStamp, _settings.MaxObjectIdleTimeSpanInSeconds);
            return isNew && isActive;
        }

        private void RememberUsageTimestamp(TK key, TV activePoolObject)
        {
            _objectToLifetimeData.AddOrUpdate(activePoolObject,
                                              new ObjectLifetimeData<TK>(key),
                                              (poolObject, previousData) => previousData.GetUpdated());
        }

        private bool NowIsClose(DateTime timestamp, int? allowedDifferenceInSeconds)
        {
            return !allowedDifferenceInSeconds.HasValue
                   || DateTime.Now - timestamp < TimeSpan.FromSeconds(allowedDifferenceInSeconds.Value);
        }

        private void OnObjectIsGone(object sender, GoneObjectEventArgs<TK, TV> goneObjectArgs)
        {
            if (goneObjectArgs.Reporter != this)
                _objectToLifetimeData.TryRemove(goneObjectArgs.PoolObject, out _notInterestingOutData);
        }

        private void InitCleaningTimer()
        {
            _cleaningTimer = new Timer(_settings.TimeSpanBetweenRevivalsInSeconds * 1000);
            _cleaningTimer.Elapsed += (sender, args) => DropLifelessObjectsAndWakeupOthers();
            _cleaningTimer.Start();
        }
    }
}
