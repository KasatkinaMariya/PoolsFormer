using System;
using System.CodeDom;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using ExampleStructures.ThreadPool;
using ExampleStructures.ThreadPool.Notification;
using PoolsLibrary;
using PoolsLibrary.Pool.BasicFunctionality;
using PoolsLibrary.Pool.Wrappers.StateMonitoring;

namespace ExampleStructures
{
    public class Program
    {
        public static void Main()
        {
            var settings = new ThreadPoolSettings
            {
                BalancingStrategy = LoadBalancingStrategy.DistributedAmongAllObjects,
                MonitoringSettings = new PWObjectStateMonitoringSettings
                {
                    TimeSpanBetweenRevivalsInSeconds = 60,
                    //MaxObjectIdleTimeSpanInSeconds = 1,
                    //MaxObjectLifetimeInSeconds = 10,
                },
                WaitingSettings = new ThreadPoolWaitingSettings
                {
                    OneWaitingTimespanInSeconds = 5,
                    WaitingsNumber = 0,
                },
                AssignAlreadyQueuedTasksBeforeDisposing = true,
                CompleteStartedTaskBeforeDisposing = true,
                MaxWorkersCount = 1,
            };

            Action<string, int, int> print = (beginning, count, mills) =>
            {
                for (int i = 0; i < count; i++)
                {
                    //Console.WriteLine(Thread.CurrentThread.ManagedThreadId + " " + beginning + i);
                    SharedEnvironment.Log.Debug(beginning + i);
                    Thread.Sleep(mills);
                }
            };

            try
            {
                var pool = new ThreadPool.ThreadPool(settings, new log4netTaskResultNotifier());

                SharedEnvironment.Log.Debug("Task1 is about to be queued");
                pool.QueueTask(() => print("aaaa", 20, 1));
                SharedEnvironment.Log.Debug("Task1 queued");

                //Thread.Sleep(3000);

                SharedEnvironment.Log.Debug("Task2 is about to be queued");
                pool.QueueTask(() => print("ffff", 10, 2));
                SharedEnvironment.Log.Debug("Task2 queued");

                SharedEnvironment.Log.Debug("Throw task is about to be queued");
                pool.QueueTask(() =>
                {
                    //Console.WriteLine(Thread.CurrentThread.ManagedThreadId + " will throw");
                    SharedEnvironment.Log.Debug("Will throw");
                    throw new FileNotFoundException();
                });
                SharedEnvironment.Log.Debug("Throw task queued");

                //Thread.Sleep(5000);

                //Thread.Sleep(500);
                //SharedEnvironment.Log.Debug("Waiting finished");

                pool.Dispose();
                SharedEnvironment.Log.Debug("Pool disposed");
            }
            catch (AggregateException e)
            {
                SharedEnvironment.Log.DebugFormat("{0} exception aggregated", e.InnerExceptions.Count);
                e.Handle(x =>
                {
                    SharedEnvironment.Log.Debug(string.Empty,e);
                    //Console.WriteLine(x);
                    return true;
                });
            }
            catch (Exception e)
            {
                SharedEnvironment.Log.Debug(string.Empty, e);
            }
            finally
            {
                SharedEnvironment.Log.Debug("ready");
                Console.ReadLine();
            }
        }
    }
}
