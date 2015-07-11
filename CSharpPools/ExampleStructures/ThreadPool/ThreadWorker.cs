using System;
using System.Threading;
using ExampleStructures.ThreadPool.Notification;
using PoolsLibrary;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.Pool.Wrappers.AutoReleasing;

namespace ExampleStructures.ThreadPool
{
    class ThreadWorker : ISelfSufficientObject<ThreadWorker>,
                         IValidnessCheckable, IDisposable
    {
        public int? Id { get { return _settings.Id; } }
        public event EventHandler<ReadyToBeReleasedEventArgs<ThreadWorker>> ReadyToBeReleased;

        private readonly ThreadWorkerSettings _settings;
        private readonly Thread _thread;
        private ThreadPoolTask _currentTask;
        private readonly AutoResetEvent _taskAssignedEvent = new AutoResetEvent(false);
        private bool _workerShouldBeStopped = false;

        private readonly IThreadPoolTaskResultNotifier _taskResultNotifier;

        private static readonly ThreadPoolTask _stopTask = new ThreadPoolTask
        {
            ActionDelegate = () => {},
            Identifier = 0,
            Name = "StopWorkerTask",
        };
        private const ThreadState _badStatesMask = ThreadState.AbortRequested | ThreadState.Aborted
                                                  | ThreadState.StopRequested | ThreadState.Stopped;

        public ThreadWorker(ThreadWorkerSettings settings, IThreadPoolTaskResultNotifier taskResultNotifier = null)
        {
            _settings = settings;
            _taskResultNotifier = taskResultNotifier;

            _thread = new Thread(ExecuteAssignedTask);
            _thread.Start();
        }

        public void AssignTask(ThreadPoolTask task)
        {
            _currentTask = task;
            AddDebugEntryToLog("task {0} has been assigned", task.ViewIdentifier);

            _taskAssignedEvent.Set();
            AddDebugEntryToLog("task assigned event has been set");
        }

        public bool IsValid()
        {
            return (_thread.ThreadState & _badStatesMask) == 0;
        }

        public void Dispose()
        {
            AddDebugEntryToLog("disposing has started");

            if (_settings.CompleteStartedTaskSafely)
            {
                _workerShouldBeStopped = true;
                if (_currentTask == null)
                    AssignTask(_stopTask);
            }
            else
            {
                _thread.Abort();
            }

            _thread.Join();
            AddDebugEntryToLog("worker's thread has joined");
        }

        private void ExecuteAssignedTask()
        {
            while (!_workerShouldBeStopped)
            {
                AddDebugEntryToLog("beginning of cycle");

                _taskAssignedEvent.WaitOne();
                AddDebugEntryToLog("task event has happened and has been reseted");

                if (ReferenceEquals(_currentTask, _stopTask))
                    return;

                try
                {
                    _currentTask.ActionDelegate();

                    if (_taskResultNotifier != null)
                        _taskResultNotifier.Notify(new ThreadPoolTaskResult
                        {
                            Task = _currentTask,
                            Succeeded = true,
                        });
                }
                catch (Exception e)
                {
                    if (_taskResultNotifier != null)
                        _taskResultNotifier.Notify(new ThreadPoolTaskResult
                        {
                            Task = _currentTask,
                            Succeeded = false,
                            Exception = e,
                        });
                }
                finally
                {
                    _currentTask = null;

                    if (ReadyToBeReleased != null)
                        ReadyToBeReleased(this, new ReadyToBeReleasedEventArgs<ThreadWorker>
                        {
                            PoolObject = this,
                        });
                }

            }
        }

        private void AddDebugEntryToLog(string message, params object[] args)
        {
            var identifiedMessage = string.Format("Worker #{0}: {1}", Id, message);
            SharedEnvironment.Log.DebugFormat(identifiedMessage, args);
        }
    }
}