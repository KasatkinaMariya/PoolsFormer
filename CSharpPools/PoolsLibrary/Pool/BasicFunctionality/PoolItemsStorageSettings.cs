namespace PoolsLibrary.Pool.BasicFunctionality
{
    public class PoolItemsStorageSettings
    {
        public LoadBalancingStrategy BalancingStrategy { get; set; }
        public bool AllowOnlyOneUserPerObject { get; set; }

        public int MaxObjectsCountPerKey { get; set; }
        public bool ThrowIfCantCreateObjectBecauseOfReachedLimit { get; set; }
    }
}