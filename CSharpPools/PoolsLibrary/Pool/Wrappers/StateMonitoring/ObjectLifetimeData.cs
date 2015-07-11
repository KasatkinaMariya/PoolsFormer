using System;

namespace PoolsLibrary.Pool.Wrappers.StateMonitoring
{
    class ObjectLifetimeData<TK>
    {
        public TK Key { get; private set; }
        public DateTime CreationTimeStamp { get; internal set; }
        public DateTime LastUsageTimeStamp { get; internal set; }

        private const string _debugDatetimeFormat = "HH:mm:ss:fff";

        public ObjectLifetimeData(TK key)
        {
            Key = key;
            CreationTimeStamp = DateTime.Now;
            LastUsageTimeStamp = DateTime.Now;
        }

        public ObjectLifetimeData<TK> GetUpdated()
        {
            return new ObjectLifetimeData<TK>(Key)
            {
                CreationTimeStamp = this.CreationTimeStamp,
                LastUsageTimeStamp = DateTime.Now,
            };
        }

        public override string ToString()
        {
            return string.Format("C='{0}' LU='{1}'",
                CreationTimeStamp.ToString(_debugDatetimeFormat),
                LastUsageTimeStamp.ToString(_debugDatetimeFormat));
        }
    }
}