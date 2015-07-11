namespace ExampleStructures.ThreadPool.Notification
{
    public interface IThreadPoolTaskResultNotifier
    {
        void Notify(ThreadPoolTaskResult taskResult);
    }
}