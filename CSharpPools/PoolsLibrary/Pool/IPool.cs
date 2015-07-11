using System;

namespace PoolsLibrary.Pool
{
    public interface IPool<TK, TV> : IDisposable
    {
        bool TryObtain(TK key, out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable);
        void Release(TK key, TV objectToRelease);
    }
}