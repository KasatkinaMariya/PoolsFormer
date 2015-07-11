using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.ThreadPool
{
    public class ThreadPoolSettings
    {
        public int MaxWorkersCount { get; set; }

        public bool AssignAlreadyQueuedTasksBeforeDisposing { get; set; }
        public bool CompleteStartedTaskBeforeDisposing { get; set; }

        public LoadBalancingStrategy BalancingStrategy { get; set; }
        public ThreadPoolWaitingSettings WaitingSettings { get; set; }

        public int MonitorTimeSpanInSeconds { get; set; }
        public int? MaxObjectLifetimeInSeconds { get; set; }
        public int? MaxObjectIdleTimeSpanInSeconds { get; set; }
    }
}