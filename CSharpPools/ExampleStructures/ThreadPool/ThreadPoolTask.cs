using System;

namespace ExampleStructures.ThreadPool
{
    public class ThreadPoolTask
    {
        public Action ActionDelegate { get; set; }
        public string ViewIdentifier
        {
            get { return Name ?? "#" + Identifier; }
        }

        public int Identifier { private get; set; }
        public string Name { private get; set; }
    }
}
