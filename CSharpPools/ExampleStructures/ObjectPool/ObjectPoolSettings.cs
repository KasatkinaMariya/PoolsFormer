using System;
using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.ObjectPool
{
    public class ObjectPoolSettings<TK,TV>
    {
        public Func<TK, TV> DefaultCreateDelegate { get; set; }
        
        public int MaxObjectCountPerKey { get; set; }
        public bool ThrowIfCantCreateBecauseOfReachedLimit { get; set; }

        public LoadBalancingStrategy BalancingStrategy { get; set; }

        public int MonitorTimeSpanInSeconds { get; set; }
        public int? MaxObjectLifetimeInSeconds { get; set; }
        public int? MaxObjectIdleTimeSpanInSeconds { get; set; }
    }
}