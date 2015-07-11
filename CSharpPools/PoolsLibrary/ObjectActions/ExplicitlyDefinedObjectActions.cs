using System;

namespace PoolsLibrary.ObjectActions
{
    public class ExplicitlyDefinedObjectActions<TV>
    {
        public Func<TV,bool> IsValidDelegate { get; set; }
        public Action<TV> PingDelegate { get; set; }
        public Action<TV> ResetDelegate { get; set; }
        public Action<TV> DisposeDelegate { get; set; }
    }
}