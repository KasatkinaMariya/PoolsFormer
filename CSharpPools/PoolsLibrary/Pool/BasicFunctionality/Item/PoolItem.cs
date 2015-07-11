using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using PoolsLibrary.Controller;
using PoolsLibrary.ObjectActions;
using PoolsLibrary.ObjectUtilization;
using PoolsLibrary.Pool.BasicFunctionality.Storages;

namespace PoolsLibrary.Pool.BasicFunctionality.Item
{
    class PoolItem<TK, TV> : IDisposable
    {
        public IStorage<TV> AvailableObjects { get; set; }
        public int NotAvailableObjectsCount
        {
            get { return _notAvailableObjects.Count; }
        }
        public int AllObjectsCount
        {
            get { return NotAvailableObjectsCount + AvailableObjects.Count; }
        }

        private readonly PoolItemSettings<TK> _settings;
        private readonly HashSet<TV> _notAvailableObjects = new HashSet<TV>();
        private readonly HashSet<TV> _objectsToKill = new HashSet<TV>();
        private readonly object _syncObject = new object();

        private readonly IPoolObjectActions<TV> _objectActions;
        private readonly IObjectUtilizer<TK, TV> _objectUtilizer;

        public PoolItem(PoolItemSettings<TK> settings,
                        IStorage<TV> availableObjectsStorage,
                        IPoolObjectActions<TV> objectActions,
                        IObjectUtilizer<TK, TV> objectUtilizer)
        {
            CheckConstructorArguments(settings, availableObjectsStorage, objectActions, objectUtilizer);

            _settings = settings;
            AvailableObjects = availableObjectsStorage;
            _objectActions = objectActions;
            _objectUtilizer = objectUtilizer;
        }

        public bool TryObtain(out TV outPoolObject, Func<TK, TV> createDelegateIfNoObjectIsAvailable)
        {
            SharedEnvironment.Log.DebugFormat("Obtaining has started. Delegate='{0}'", createDelegateIfNoObjectIsAvailable);

            lock (_syncObject)
            {
                SharedEnvironment.Log.DebugFormat("Obtaining has acquired lock. {0} total, {1} idle",
                                                   AllObjectsCount, AvailableObjects.Count);

                if (!TryProvideExistingObject(out outPoolObject) &&
                    !TryProvideNewObject(createDelegateIfNoObjectIsAvailable, out outPoolObject))
                {
                    outPoolObject = default(TV);
                    return false;
                }
                SharedEnvironment.Log.DebugFormat("Obtaining has got object. {0} total, {1} idle",
                                                   AllObjectsCount, AvailableObjects.Count);

                if (_settings.MarkObtainedObjectAsNotAvailable)
                    _notAvailableObjects.Add(outPoolObject);
                else
                    AvailableObjects.Add(outPoolObject);
                SharedEnvironment.Log.DebugFormat("Obtaining has marked object. {0} total, {1} idle",
                                                   AllObjectsCount, AvailableObjects.Count);
            }

            SharedEnvironment.Log.DebugFormat("Obtaining has released lock");
            return true;
        }

        public void Release(TV objectToUnmark)
        {
            SharedEnvironment.Log.Debug("Releasing has started");

            if (!_settings.MarkObtainedObjectAsNotAvailable)
            {
                var markingIsOffMessage = "Operation of marking object as available is invalid because marking was ordered to be off";
                throw new InvalidPoolOperationException<TK, TV>(objectToUnmark, _settings.Key, markingIsOffMessage);
            }

            lock (_syncObject)
            {
                SharedEnvironment.Log.DebugFormat("Releasing has acquired lock. {0} total, {1} idle",
                                                   AllObjectsCount, AvailableObjects.Count);

                if (_notAvailableObjects.Remove(objectToUnmark))
                {
                    SharedEnvironment.Log.Debug("Object has been removed from busy objects collection");
                    if (!KilledBecauseItIsBad(objectToUnmark))
                    {
                        AvailableObjects.Add(objectToUnmark);
                        SharedEnvironment.Log.Debug("Object has been added to free objects collection");
                    }

                    SharedEnvironment.Log.DebugFormat("Releasing is about to release lock. {0} total, {1} idle",
                                                       AllObjectsCount, AvailableObjects.Count);
                    return;
                }

                var invalidUnmarkOperationMessage = AvailableObjects.Contains(objectToUnmark)
                    ? "Marking object as available has been declined because it's currently available. Object should be marked as not available first"
                    : "Marking object as available has been declined because this object wasn't created by pool, it's a stranger";
                throw new InvalidPoolOperationException<TK, TV>(objectToUnmark, _settings.Key, invalidUnmarkOperationMessage);
            }
        }

        public void MarkObjectForKilling(TV toKill)
        {
            lock (_syncObject)
            {
                if (_notAvailableObjects.Contains(toKill) || AvailableObjects.Contains(toKill))
                    _objectsToKill.Add(toKill);
            }
        }

        public void Dispose()
        {
            SharedEnvironment.Log.DebugFormat("Disposing has started. {0} total, {1} idle",
                                               AllObjectsCount, AvailableObjects.Count);

            while (AvailableObjects.Count > 0)
                KillObject(AvailableObjects.Remove());
            SharedEnvironment.Log.Debug("Idle objects have been killed");

            foreach (var notAvailableObject in _notAvailableObjects)
                KillObject(notAvailableObject);
            SharedEnvironment.Log.Debug("Busy objects have been killed");
        }

        private bool TryProvideExistingObject(out TV outPoolObject)
        {
            while (AvailableObjects.Count > 0)
            {
                var availableObject = AvailableObjects.Remove();

                if (KilledBecauseItIsBad(availableObject))
                    continue;

                outPoolObject = availableObject;
                return true;
            }

            outPoolObject = default(TV);
            return false;
        }

        private bool TryProvideNewObject(Func<TK, TV> createDelegate, out TV outPoolObject)
        {
            if (AllObjectsCount == _settings.MaxObjectsCount)
                if (_settings.ThrowIfCantCreateNewBecauseOfReachedLimit)
                {
                    var limitReachedMessage = string.Format("Object with key='{0}' wasn't created because " +
                                                            "max objects count {1} is already reached",
                                                            _settings.Key, _settings.MaxObjectsCount);
                    throw new ObjectsMaxCountReachedException<TK>(_settings.Key, _settings.MaxObjectsCount, limitReachedMessage);
                }
                else
                {
                    outPoolObject = default(TV);
                    return false;
                }

            if (createDelegate == null)
            {
                outPoolObject = default(TV);
                return false;
            }

            TV newlyCreatedObject;
            try
            {
                newlyCreatedObject = createDelegate(_settings.Key);
            }
            catch (Exception e)
            {
                var creationThrewMessage = string.Format("Creation object with key='{0}' failed. Look at inner exception for details",
                                                         _settings.Key);
                throw new ObjectCreationFailedException<TK, TV>(_settings.Key, createDelegate, creationThrewMessage, e);
            }

            if (_objectActions.IsValid(newlyCreatedObject))
            {
                outPoolObject = newlyCreatedObject;
                return true;
            }

            _objectActions.Dispose(newlyCreatedObject);
            var invalidObjectCreatedMessage = string.Format("Provided delegate created invalid object with key='{0}'",
                                                            _settings.Key);
            throw new ObjectCreationFailedException<TK, TV>(_settings.Key, createDelegate, invalidObjectCreatedMessage);
        }

        private bool KilledBecauseItIsBad(TV toCheck)
        {
            if (!_objectsToKill.Remove(toCheck) && _objectActions.IsValid(toCheck))
                return false;

            KillObject(toCheck);
            return true;
        }

        private void KillObject(TV toKill)
        {
            _objectActions.Dispose(toKill);
            _objectUtilizer.Utilize(_settings.Key, toKill, this);
        }

        private void CheckConstructorArguments(PoolItemSettings<TK> settings,
                                       IStorage<TV> availableObjectsStorage,
                                       IPoolObjectActions<TV> objectActions,
                                       IObjectUtilizer<TK, TV> objectUtilizer)
        {
            if (settings == null)
                throw new ArgumentNullException("settings");

            if (settings.Key == null)
                throw new ArgumentException("PoolItemSettings should contain key", "settings.Key");

            if (availableObjectsStorage == null)
                throw new ArgumentNullException("availableObjectsStorage");

            if (objectActions == null)
                throw new ArgumentNullException("objectActions");

            if (objectUtilizer == null)
                throw new ArgumentNullException("objectUtilizer");
        }
    }
}