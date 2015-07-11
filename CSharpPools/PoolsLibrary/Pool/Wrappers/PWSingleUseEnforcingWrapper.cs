using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.Pool.BasicFunctionality.Item;

namespace PoolsLibrary.Pool.Wrappers
{
    public class PWSingleUseEnforcingWrapper<TK,TV> : IInternalPool<TK,TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> IInternalPool<TK, TV>.KeyToPoolItem
        {
            get { return _basePool.KeyToPoolItem; }
        }

        private readonly IInternalPool<TK, TV> _basePool;

        public PWSingleUseEnforcingWrapper(IPool<TK, TV> basePool)
        {
            _basePool = basePool as IInternalPool<TK,TV>;
        }

        public bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            return _basePool.TryObtain(key, out outPoolObject, createDelegateIfNoObjectIsAvailable);
        }

        public void Release(TK key, TV objectToRelease)
        {
            PoolItem<TK, TV> outPoolItem;
            ((IInternalPool<TK,TV>)this).KeyToPoolItem.TryGetValue(key, out outPoolItem);
            outPoolItem.Release(objectToRelease);

            _basePool.Release(key, objectToRelease);
        }

        public void Dispose()
        {
            _basePool.Dispose();
        }
    }
}
