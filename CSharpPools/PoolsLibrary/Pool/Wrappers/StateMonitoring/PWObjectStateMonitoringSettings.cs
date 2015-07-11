namespace PoolsLibrary.Pool.Wrappers.StateMonitoring
{
    public class PWObjectStateMonitoringSettings
    {
        public int TimeSpanBetweenRevivalsInSeconds { get; set; }
        public int? MaxObjectLifetimeInSeconds { get; set; }
        public int? MaxObjectIdleTimeSpanInSeconds { get; set; }
    }
}