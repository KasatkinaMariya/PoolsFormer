using System;

namespace PoolsLibrary.Pool.Wrappers.AutoReleasing
{
    public interface ISelfSufficientObject<TV>
    {
        event EventHandler<ReadyToBeReleasedEventArgs<TV>> ReadyToBeReleased;
    }
}