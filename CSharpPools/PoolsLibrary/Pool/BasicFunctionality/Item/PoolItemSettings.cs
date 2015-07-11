namespace PoolsLibrary.Pool.BasicFunctionality.Item
{
    class PoolItemSettings<TK>
    {
        public TK Key { get; set; }
        public bool MarkObtainedObjectAsNotAvailable { get; set; }

        public int MaxObjectsCount { get; set; }
        public bool ThrowIfCantCreateNewBecauseOfReachedLimit { get; set; }
    }
}
