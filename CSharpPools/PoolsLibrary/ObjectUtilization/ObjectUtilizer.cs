using System;

namespace PoolsLibrary.ObjectUtilization
{
    public class ObjectUtilizer<TK, TV> : IObjectUtilizer<TK, TV>
    {
        public event EventHandler<GoneObjectEventArgs<TK, TV>> ObjectIsGone;

        public void Utilize(TK key, TV pooObject, object caller)
        {
            if (ObjectIsGone != null)
                ObjectIsGone(this, new GoneObjectEventArgs<TK, TV>
                {
                    Key = key,
                    PoolObject = pooObject,
                    Reporter = caller,
                });
        }
    }
}