namespace PoolsLibrary.ObjectActions
{
    public interface IPoolObjectActions<in TV>
    {
        bool IsValid(TV poolObject);
        bool Ping(TV poolObject);
        bool Reset(TV poolObject);
        void Dispose(TV poolObject);
    }
}