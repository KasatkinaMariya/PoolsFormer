using System;
using System.Collections.Generic;
using System.Configuration;
using System.Threading;
using ExampleStructures.ThreadPool.Notification;
using PoolsLibrary;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers;
using PoolsLibrary.Pool.Wrappers.AutoReleasing;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures.ThreadPool
{
    public class ThreadPool : IDisposable
    {
        private readonly ThreadPoolSettings _settings;
        private readonly PoolController<int, ThreadWorker> _poolController;
        private readonly DirectionIfNoObjectIsAvailable<int, ThreadWorker> _noIdleWorkerDirection;

        private readonly Queue<ThreadPoolTask> _tasksQueue = new Queue<ThreadPoolTask>();
        private readonly object _tasksQueueSyncObject = new object();
        private readonly ManualResetEvent _queueIsNotEmptyEvent = new ManualResetEvent(false);

        private readonly Thread _managerThread;
        private bool _threadPoolShouldBeStopped = false;

        private readonly IThreadPoolTaskResultNotifier _taskResultNotifier;

        private const int _fakeKey = 1;
        private static int _previousTaskNumber;
        private static int _previousWorkerNumber;

        public ThreadPool(ThreadPoolSettings settings, IThreadPoolTaskResultNotifier taskResultNotifier = null)
        {
            _settings = settings;
            _taskResultNotifier = taskResultNotifier;
            _poolController = CreateThreadPoolControllerInstance();

            _noIdleWorkerDirection = new DirectionIfNoObjectIsAvailable<int, ThreadWorker>
            {
                CreateDelegateIfNoObjectIsAvailable = CreateWorker,
                AttemptsNumber = _settings.WaitingSettings != null
                                 ? _settings.WaitingSettings.WaitingsNumber + 1
                                 : 1,
                OneIntervalBetweenAttemptsInSeconds = _settings.WaitingSettings != null
                                                      ? _settings.WaitingSettings.OneWaitingTimespanInSeconds
                                                      : 0,
            };

            _managerThread = new Thread(AssignTasksToWorkers)
            {
                Name = ConfigurationManager.AppSettings["managerThreadName"],
                IsBackground = true,
            };
            _managerThread.Start();
        }

        public string QueueTask(Action task, string name = null)
        {
            var poolTask = new ThreadPoolTask
            {
                ActionDelegate = task,
                Identifier = Interlocked.Increment(ref _previousTaskNumber),
                Name = name,
            };

            lock (_tasksQueueSyncObject)
                _tasksQueue.Enqueue(poolTask);
            _queueIsNotEmptyEvent.Set();
            SharedEnvironment.Log.Debug("Queue event has been set");

            return poolTask.ViewIdentifier;
        }

        public void Dispose()
        {
            SharedEnvironment.Log.Debug("Disposing has started");
            _threadPoolShouldBeStopped = true;
            SharedEnvironment.Log.Debug("Manager thread has received command to stop");

            if (!_queueIsNotEmptyEvent.WaitOne(0))
            {
                _queueIsNotEmptyEvent.Set();
                SharedEnvironment.Log.Debug("Queue event has been just set");
            }
            else
            {
                SharedEnvironment.Log.Debug("Queue event has been already set");
            }

            _managerThread.Join();
            SharedEnvironment.Log.Debug("Manager thread has joined");

            _poolController.Dispose();
            SharedEnvironment.Log.Debug("Controller disposed");
        }

        private void AssignTasksToWorkers()
        {
            while (!_threadPoolShouldBeStopped
                   || (_settings.AssignAlreadyQueuedTasksBeforeDisposing && _tasksQueue.Count > 0))
            {
                SharedEnvironment.Log.Debug("Beginning of cycle");

                if (_tasksQueue.Count == 0)
                {
                    SharedEnvironment.Log.Debug("Waiting for new tasks");
                    _queueIsNotEmptyEvent.WaitOne();
                    SharedEnvironment.Log.Debug("Queue event has happened");
                }
                else
                {
                    SharedEnvironment.Log.DebugFormat("Tasks count in queue: {0}", _tasksQueue.Count);
                }

                var currentTask = _tasksQueue.Dequeue();
                SharedEnvironment.Log.DebugFormat("Current task is {0}", currentTask.ViewIdentifier);

                ThreadWorker worker;
                if (_poolController.Obtain(_fakeKey, out worker, _noIdleWorkerDirection))
                {
                    worker.AssignTask(currentTask);
                    SharedEnvironment.Log.DebugFormat("Task {0} has been assigned to worker {1}",
                                                       currentTask.ViewIdentifier, worker.Id);
                }
                else
                {
                    if (_taskResultNotifier != null)
                        _taskResultNotifier.Notify(new ThreadPoolTaskResult
                        {
                            Task = currentTask,
                            Succeeded = false,
                            Exception = new Exception("No available workers"),
                        });
                }

                _queueIsNotEmptyEvent.Reset();
            }
        }

        private ThreadWorker CreateWorker(int key)
        {
            var settings = new ThreadWorkerSettings
            {
                Id = Interlocked.Increment(ref _previousWorkerNumber),
                CompleteStartedTaskSafely = _settings.CompleteStartedTaskBeforeDisposing,
            };
            return new ThreadWorker(settings, _taskResultNotifier);
        }

        private PoolController<int, ThreadWorker> CreateThreadPoolControllerInstance()
        {
            var workerActions = new ObjectActionsBasedOnDelegateOrInterface<ThreadWorker>(new ExplicitlyDefinedObjectActions<ThreadWorker>());
            var objectUtilizer = new ObjectUtilizer<int, ThreadWorker>();

            var basicPoolSettings = new PoolItemsStorageSettings
            {
                AllowOnlyOneUserPerObject = true,
                BalancingStrategy = _settings.BalancingStrategy,
                MaxObjectsCountPerKey = _settings.MaxWorkersCount,
                ThrowIfCantCreateObjectBecauseOfReachedLimit = false,
            };
            var basicPool = new PoolItemsStorage<int, ThreadWorker>(basicPoolSettings, workerActions, objectUtilizer);

            var stateMonitoringSettings = new PWObjectStateMonitoringSettings
            {
                MaxObjectIdleTimeSpanInSeconds = _settings.MaxObjectIdleTimeSpanInSeconds,
                MaxObjectLifetimeInSeconds = _settings.MaxObjectIdleTimeSpanInSeconds,
                TimeSpanBetweenRevivalsInSeconds = _settings.MonitorTimeSpanInSeconds,
            };
            var stateMonitoringPool = new PWObjectStateMonitoringWrapper<int, ThreadWorker>(stateMonitoringSettings,
                                                                                            basicPool,
                                                                                            workerActions,
                                                                                            objectUtilizer);

            var singleUsePool = new PWSingleUseEnforcingWrapper<int, ThreadWorker>(stateMonitoringPool);

            var autoReleasingPool = new PWAutoReleasingWrapper<int, ThreadWorker>(singleUsePool);

            var poolControllerSettings = new PoolControllerSettings
            {
                CallingReleaseOperationWillHappen = true,
            };
            var controller = new PoolController<int, ThreadWorker>(poolControllerSettings, autoReleasingPool);
            autoReleasingPool.SetPoolController(controller);
            return controller;
        }
    }
}
