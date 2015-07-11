using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality.Item;

namespace PoolsLibrary.Pool.Wrappers
{
    public class PWObjectResettingWrapper<TK,TV> : IInternalPool<TK,TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> IInternalPool<TK, TV>.KeyToPoolItem
        {
            get { return _basePool.KeyToPoolItem; }
        }

        private readonly IInternalPool<TK, TV> _basePool;
        private readonly IPoolObjectActions<TV> _objectActions;
        private readonly IObjectUtilizer<TK, TV> _objectUtilizer;

        public PWObjectResettingWrapper(IPool<TK, TV> basePool,
                                        IPoolObjectActions<TV> objectActions,
                                        IObjectUtilizer<TK,TV> objectUtilizer)
        {
            _basePool = basePool as IInternalPool<TK,TV>;
            _objectActions = objectActions;
            _objectUtilizer = objectUtilizer;
        }

        public bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            return _basePool.TryObtain(key, out outPoolObject, createDelegateIfNoObjectIsAvailable);
        }

        public void Release(TK key, TV objectToRelease)
        {
            var resetSucceeded = _objectActions.Reset(objectToRelease);
            if (!resetSucceeded)
                _objectUtilizer.Utilize(key,objectToRelease,this);

            _basePool.Release(key, objectToRelease);
        }

        public void Dispose()
        {
            _basePool.Dispose();
        }
    }
}
