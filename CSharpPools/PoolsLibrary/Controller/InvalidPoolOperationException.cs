namespace PoolsLibrary.Controller
{
    class InvalidPoolOperationException<TK,TV> : PoolException<TK>
    {
        public TV Object { get; private set; }

        public InvalidPoolOperationException(TV operationObject = default(TV),
                                             TK key = default(TK),
                                             string message = null)
            : base(key, message)
        {
            Object = operationObject;
        }

        public InvalidPoolOperationException()
        {
        }
    }
}
