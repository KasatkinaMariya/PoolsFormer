using System;

namespace PoolsLibrary.ObjectUtilization
{
    public class GoneObjectEventArgs<TK,TV> : EventArgs
    {
        public object Reporter { get; set; }
        public TK Key { get; set; }
        public TV PoolObject { get; set; }
    }
}