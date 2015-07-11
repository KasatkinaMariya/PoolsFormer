using System;

namespace UnitTests.TestEntities
{
    class CreateDelegateWithCounter
    {
        public Func<TestKey, TestResource> Delegate { get; private set; }
        public int NumberOfCallings { get; private set; }

        private int _numberToStartFrom;

        public CreateDelegateWithCounter(int numberToStartFrom)
        {
            _numberToStartFrom = numberToStartFrom - 1;

            Delegate = key =>
            {
                NumberOfCallings++;
                _numberToStartFrom++;
                return new TestResource(_numberToStartFrom.ToString());
            };
        }
    }
}