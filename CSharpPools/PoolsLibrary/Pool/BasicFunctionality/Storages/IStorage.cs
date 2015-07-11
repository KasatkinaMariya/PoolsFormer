namespace PoolsLibrary.Pool.BasicFunctionality.Storages
{
    interface IStorage<TV>
    {
        int Count { get; }

        void Add(TV toAdd);
        TV Remove();
        bool Contains(TV toFind);
    }
}