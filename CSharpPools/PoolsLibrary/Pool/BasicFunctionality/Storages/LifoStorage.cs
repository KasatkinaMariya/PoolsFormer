using System.Collections.Generic;

namespace PoolsLibrary.Pool.BasicFunctionality.Storages
{
    class LifoStorage<TV> : StorageBase<TV>
    {
        public override int Count
        {
            get { return _stack.Count; }
        }

        private readonly Stack<TV> _stack = new Stack<TV>();

        public override void Add(TV toAdd)
        {
            _stack.Push(toAdd);
        }

        public override TV Remove()
        {
            return _stack.Pop();
        }

        public override bool Contains(TV toFind)
        {
            return _stack.Contains(toFind);
        }
    }
}