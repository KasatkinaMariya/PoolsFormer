using System;

namespace PoolsLibrary.Pool.Wrappers.AutoReleasing
{
    public class ReadyToBeReleasedEventArgs<TV> : EventArgs
    {
        public TV PoolObject { get; set; }
    }
}