using System;
using System.Collections.Concurrent;
using System.Threading.Tasks;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality.Item;
using PoolsLibrary.Pool.BasicFunctionality.Storages;

namespace PoolsLibrary.Pool.BasicFunctionality
{
    public class PoolItemsStorage<TK, TV> : IInternalPool<TK, TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> IInternalPool<TK, TV>.KeyToPoolItem
        {
            get { return _keyToPoolItem; }
        }

        private readonly PoolItemsStorageSettings _settings;
        private readonly ConcurrentDictionary<TK, PoolItem<TK, TV>> _keyToPoolItem;

        private readonly IPoolObjectActions<TV> _objectActions;
        private readonly IObjectUtilizer<TK, TV> _objectUtilizer;

        public PoolItemsStorage(PoolItemsStorageSettings settings,
                        IPoolObjectActions<TV> objectActions,
                        IObjectUtilizer<TK, TV> objectUtilizer)
        {
            _settings = settings;
            _objectActions = objectActions;
            _objectUtilizer = objectUtilizer;

            _keyToPoolItem = new ConcurrentDictionary<TK, PoolItem<TK, TV>>();
            _objectUtilizer.ObjectIsGone += OnObjectIsGoneEvent;
        }

        public bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            var poolItem = _keyToPoolItem.GetOrAdd(key, CreatePoolItem);
            return poolItem.TryObtain(out outPoolObject, createDelegateIfNoObjectIsAvailable);
        }

        public void Release(TK key, TV objectToRelease)
        {
        }

        public void Dispose()
        {
            _objectUtilizer.ObjectIsGone -= OnObjectIsGoneEvent;
            Parallel.ForEach(_keyToPoolItem.Values, x => x.Dispose());
        }

        private void OnObjectIsGoneEvent(object sender, GoneObjectEventArgs<TK, TV> goneObjectArgs)
        {
            if (goneObjectArgs.Reporter.GetType() == typeof (PoolItem<TK, TV>))
                return;

            PoolItem<TK, TV> outPoolItem;
            if (_keyToPoolItem.TryGetValue(goneObjectArgs.Key, out outPoolItem))
                outPoolItem.MarkObjectForKilling(goneObjectArgs.PoolObject);
        }

        private PoolItem<TK, TV> CreatePoolItem(TK key)
        {
            var poolItemSettings = new PoolItemSettings<TK>
            {
                Key = key,
                MarkObtainedObjectAsNotAvailable = _settings.AllowOnlyOneUserPerObject,
                MaxObjectsCount = _settings.MaxObjectsCountPerKey,
                ThrowIfCantCreateNewBecauseOfReachedLimit = _settings.ThrowIfCantCreateObjectBecauseOfReachedLimit,
            };
            var availableObjectsStorage = StorageBase<TV>.Create(_settings.BalancingStrategy);

            return new PoolItem<TK, TV>(poolItemSettings, availableObjectsStorage, _objectActions, _objectUtilizer);
        }
    }
}
