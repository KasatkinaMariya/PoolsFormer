using System;

namespace PoolsLibrary.Controller
{
    public class DirectionIfNoObjectIsAvailable<TK, TV>
    {
        public int AttemptsNumber
        {
            get { return _attempsNumber; }
            set
            {
                if (value <= 0)
                    throw new ArgumentOutOfRangeException("AttemptsNumber", value,
                                                          "Number of attemps must be positive");
                _attempsNumber = value;
            }
        }
        private int _attempsNumber;

        public int OneIntervalBetweenAttemptsInSeconds
        {
            get { return _oneIntervalBetweenAttemptsInSeconds; }
            set
            {
                if (value < 0)
                    throw new ArgumentOutOfRangeException("OneIntervalBetweenAttemptsInSeconds", value,
                                                          "Interval between attempts must me equal to zero or positive");
                _oneIntervalBetweenAttemptsInSeconds = value;
            }

        }
        private int _oneIntervalBetweenAttemptsInSeconds;

        public Func<TK, TV> CreateDelegateIfNoObjectIsAvailable { get; set; }

        public static DirectionIfNoObjectIsAvailable<TK, TV> DoNotWaitDirection
        {
            get
            {
                return new DirectionIfNoObjectIsAvailable<TK, TV>
                {
                    AttemptsNumber = 1,
                };
            }
        }
    }
}