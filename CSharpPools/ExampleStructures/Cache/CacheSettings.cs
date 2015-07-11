using System;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.Cache
{
    public class CacheSettings<TK,TV>
    {
        public Func<TK,TV> DefaultCreateDelegate { get; set; }

        public int MonitorTimeSpanInSeconds { get; set; }
        public int? MaxObjectLifetimeInSeconds { get; set; }
        public int? MaxObjectIdleTimeSpanInSeconds { get; set; }
    }
}