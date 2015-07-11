using System;

namespace PoolsLibrary.ObjectUtilization
{
    public interface IObjectUtilizer<TK,TV>
    {
        event EventHandler<GoneObjectEventArgs<TK,TV>> ObjectIsGone;

        void Utilize(TK key, TV pooObject, object caller);
    }
}