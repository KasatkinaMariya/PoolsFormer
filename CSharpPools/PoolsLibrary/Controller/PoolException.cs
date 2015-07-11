using System;

namespace PoolsLibrary.Controller
{
    public class PoolException<TK> : Exception
    {
        public TK Key { get; private set; }

        public PoolException(TK key, string message = null, Exception innerException = null)
            : base(message,innerException)
        {
            Key = key;
        }

        public PoolException(string message = null, Exception innerException = null)
            : this (default(TK), message, innerException)
        {
        }
    }
}
