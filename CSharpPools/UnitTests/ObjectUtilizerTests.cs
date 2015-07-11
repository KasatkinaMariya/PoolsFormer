using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Moq;
using NUnit.Framework;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;

namespace UnitTests
{
    [TestFixture]
    public class ObjectUtilizerTests
    {
        private const string _key = "key";
        private const string _objectData = "pool object data";

        [Test]
        public void UtilizationWasCalled_GoneObjectEventWithObjectDataIsRaised()
        {
            var utilizer = new ObjectUtilizer<string, string>();
            var eventWasRaised = false;
            var keyFromEvent = string.Empty;
            var objectDataFromEvent = string.Empty;
            var reporter = new object();
            object reporterFromEvent = null;

            utilizer.ObjectIsGone += (sender, args) =>
            {
                eventWasRaised = true;
                keyFromEvent = args.Key;
                objectDataFromEvent = args.PoolObject;
                reporterFromEvent = args.Reporter;
            };
            utilizer.Utilize(_key, _objectData, reporter);

            Assert.That(eventWasRaised, Is.True);
            Assert.That(keyFromEvent, Is.EqualTo(_key));
            Assert.That(objectDataFromEvent, Is.EqualTo(_objectData));
            Assert.That(reporterFromEvent, Is.EqualTo(reporter));
        }

        // actions=null in constructor
    }
}
