using System;

namespace ExampleStructures.ThreadPool.Notification
{
    public class ThreadPoolTaskResult
    {
        public ThreadPoolTask Task { get; set; }
        public bool Succeeded { get; set; }
        public Exception Exception { get; set; }
    }
}