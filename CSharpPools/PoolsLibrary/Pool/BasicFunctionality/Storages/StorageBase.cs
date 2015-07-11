namespace PoolsLibrary.Pool.BasicFunctionality.Storages
{
    abstract class StorageBase<TV> : IStorage<TV>
    {
        public abstract int Count { get; }

        public abstract void Add(TV toAdd);
        public abstract TV Remove();
        public abstract bool Contains(TV toFind);

        public static IStorage<TV> Create(LoadBalancingStrategy balancingStrategy)
        {
            switch (balancingStrategy)
            {
                case LoadBalancingStrategy.DistributedAmongAllObjects:
                    return new FifoStorage<TV>();
                case LoadBalancingStrategy.IntensiveOnRecentlyUsedObjects:
                    return new LifoStorage<TV>();
                default:
                    return new FifoStorage<TV>();
            }
        }
    }
}
