using System;

namespace UnitTests.TestEntities
{
    public class TestResource
    {
        public Guid UniqueResourceIdentifier { get; private set; }
        public string Value { get; set; }

        public TestResource(string value)
        {
            Value = value;
            UniqueResourceIdentifier = Guid.NewGuid();
        }

        public override bool Equals(object obj)
        {
            var another = obj as TestResource;
            return another.Value.Equals(Value);
        }

        public override int GetHashCode()
        {
            return UniqueResourceIdentifier.GetHashCode();
        }
    }
}