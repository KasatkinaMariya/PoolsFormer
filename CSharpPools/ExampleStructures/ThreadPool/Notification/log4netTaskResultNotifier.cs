using PoolsLibrary;

namespace ExampleStructures.ThreadPool.Notification
{
    public class log4netTaskResultNotifier : IThreadPoolTaskResultNotifier
    {
        public void Notify(ThreadPoolTaskResult taskResult)
        {
            var message = string.Format("Thread pool task {0} {1}",
                                         taskResult.Task.ViewIdentifier,
                                         taskResult.Succeeded ? "succeeded" : "failed. " + taskResult.Exception);

            if (taskResult.Succeeded)
                SharedEnvironment.Log.Info(message);
            else
                SharedEnvironment.Log.Warn(message);
        }
    }
}