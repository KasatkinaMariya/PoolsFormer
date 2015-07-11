using System.Collections.Concurrent;
using PoolsLibrary.Pool.BasicFunctionality.Item;

namespace PoolsLibrary.Pool
{
    internal interface IInternalPool<TK, TV> : IPool<TK, TV>
    {
        ConcurrentDictionary<TK, PoolItem<TK, TV>> KeyToPoolItem { get; }
    }
}