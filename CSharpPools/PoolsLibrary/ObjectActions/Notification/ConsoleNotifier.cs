using System;

namespace PoolsLibrary.ObjectActions.Notification
{
    public class ConsoleNotifier : INotifier
    {
        public void Notify<TV>(UserDefinedActionError<TV> actionError)
        {
            Console.WriteLine("Error occured during user action {0}: {1}",
                               actionError.UserDefinedActionType, actionError.Exception.Message);
        }
    }
}
