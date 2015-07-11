namespace PoolsLibrary.ObjectActions.Notification
{
    public interface INotifier
    {
        void Notify<TV>(UserDefinedActionError<TV> actionError);
    }
}