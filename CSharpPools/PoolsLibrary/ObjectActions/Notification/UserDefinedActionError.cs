using System;

namespace PoolsLibrary.ObjectActions.Notification
{
    public class UserDefinedActionError<TV>
    {
        public UserDefinedActionType UserDefinedActionType { get; set; }
        public TV Object { get; set; }
        public Exception Exception { get; set; }

        public override bool Equals(object obj)
        {
            var anotherData = obj as UserDefinedActionError<TV>;
            if (anotherData == null)
                return false;

            return UserDefinedActionType == anotherData.UserDefinedActionType
                   && Object.Equals(anotherData.Object)
                   && Exception.GetType() == anotherData.Exception.GetType();
        }

        public override int GetHashCode()
        {
            return UserDefinedActionType.GetHashCode();
        }
    }
}