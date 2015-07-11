using System;
using PoolsLibrary.Controller;

namespace PoolsLibrary.Pool.BasicFunctionality.Item
{
    public class ObjectCreationFailedException<TK, TV> : PoolException<TK>
    {
        public Func<TK, TV> UsedCreateDelegate { get; private set; }

        public ObjectCreationFailedException(TK key, Func<TK,TV> usedCreateDelegate, string message = null, Exception innerException = null)
            : base(key, message, innerException)
        {
            UsedCreateDelegate = usedCreateDelegate;
        }

        public ObjectCreationFailedException()
        {
        }
    }
}
