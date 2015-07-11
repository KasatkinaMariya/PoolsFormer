using PoolsLibrary.Controller;

namespace PoolsLibrary.Pool.BasicFunctionality.Item
{
    public class ObjectsMaxCountReachedException<TK> : PoolException<TK>
    {
        public int MaxObjectsCount { get; private set; }

        public ObjectsMaxCountReachedException(TK key, int maxObjectsCount, string message = null)
            : base(key, message)
        {
            MaxObjectsCount = maxObjectsCount;
        }
    }
}