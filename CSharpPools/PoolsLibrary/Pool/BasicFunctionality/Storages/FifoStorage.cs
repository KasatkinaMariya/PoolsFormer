using System.Collections.Generic;

namespace PoolsLibrary.Pool.BasicFunctionality.Storages
{
    class FifoStorage<TV> : StorageBase<TV>
    {
        public override int Count
        {
            get { return _queue.Count; }
        }

        private readonly Queue<TV> _queue = new Queue<TV>();

        public override void Add(TV toAdd)
        {
            _queue.Enqueue(toAdd);
        }

        public override TV Remove()
        {
            return _queue.Dequeue();
        }

        public override bool Contains(TV toFind)
        {
            return _queue.Contains(toFind);
        }
    }
}
