using System;
using System.Linq;
using PoolsLibrary.ObjectActions.Notification;

namespace PoolsLibrary.ObjectActions
{
    public class ObjectActionsBasedOnDelegateOrInterface<TV> : IPoolObjectActions<TV>
    {
        private readonly Func<TV, bool> _isValidDelegate;
        private readonly Action<TV> _pingDelegate;
        private readonly Action<TV> _resetDelegate;
        private readonly Action<TV> _disposeDelegate;

        private readonly INotifier _notifier;

        public ObjectActionsBasedOnDelegateOrInterface(ExplicitlyDefinedObjectActions<TV> overridingActions,
                                                       INotifier notifier = null)
        {
            _notifier = notifier;

            _isValidDelegate = overridingActions.IsValidDelegate ?? ((typeof(IValidnessCheckable).IsAssignableFrom(typeof(TV)))
                                                                    ? (x => (x as IValidnessCheckable).IsValid())
                                                                    : (Func<TV, bool>)(x => true));
            _pingDelegate = ChooseDelegate(overridingActions.PingDelegate, typeof(IPingable));
            _resetDelegate = ChooseDelegate(overridingActions.ResetDelegate, typeof(IStateResettable));
            _disposeDelegate = ChooseDelegate(overridingActions.DisposeDelegate, typeof(IDisposable));
        }

        public bool IsValid(TV poolObject)
        {
            return ExecuteSafely(_isValidDelegate, poolObject, UserDefinedActionType.CheckingValidness);
        }

        public bool Ping(TV poolObject)
        {
            return ExecuteSafely(_pingDelegate, poolObject, UserDefinedActionType.Pinging);
        }

        public bool Reset(TV poolObject)
        {
            return ExecuteSafely(_resetDelegate, poolObject, UserDefinedActionType.Resetting);
        }

        public void Dispose(TV poolObject)
        {
            ExecuteSafely(_disposeDelegate, poolObject, UserDefinedActionType.Disposing);
        }

        private Action<TV> ChooseDelegate(Action<TV> explicitlyDefined, Type interfaceType)
        {
            if (explicitlyDefined != null)
                return explicitlyDefined;

            bool interfaceIsImplemented = typeof(TV).GetInterface(interfaceType.Name) != null;
            if (interfaceIsImplemented)
                return poolObject => interfaceType.GetMethods().First()
                                     .Invoke(poolObject, null);

            return poolObject => { };
        }

        private bool ExecuteSafely(Delegate operationDelegate, TV poolObject, UserDefinedActionType actionType)
        {
            try
            {
                var operationWithoutResult = operationDelegate as Action<TV>;
                if (operationWithoutResult != null)
                {
                    operationWithoutResult(poolObject);
                    return true;
                }

                var operationWithResult = operationDelegate as Func<TV, bool>;
                if (operationWithResult != null)
                    return operationWithResult(poolObject);

                throw new ArgumentException("Unexpected type of operation delegate: " + operationDelegate);
            }
            catch (Exception e)
            {
                if (_notifier != null)
                    _notifier.Notify(new UserDefinedActionError<TV>
                    {
                        UserDefinedActionType = actionType,
                        Object = poolObject,
                        Exception = e,
                    });
                return false;
            }
        }
    }
}
