using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using log4net;
using log4net.Config;
using log4net.Repository.Hierarchy;

namespace PoolsLibrary
{
    public static class SharedEnvironment
    {
        public static ILog Log { get; private set; }

        static SharedEnvironment()
        {
            Log = InitLog4net();
        }

        private static ILog InitLog4net()
        {
            XmlConfigurator.Configure();
            return LogManager.GetLogger(typeof(Logger));
        }
    }
}
