namespace UnitTests.TestEntities
{
    public class TestKey
    {
        public int Identifier { get; set; }

        public override bool Equals(object obj)
        {
            var another = obj as TestKey;
            return another.Identifier == Identifier;
        }

        public override int GetHashCode()
        {
            return Identifier;
        }
    }
}