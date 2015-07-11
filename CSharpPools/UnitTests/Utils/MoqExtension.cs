using System.Reflection;
using Moq.Language;
using Moq.Language.Flow;

namespace UnitTests.Utils
{
    static class MoqExtension
    {
        public delegate void OutAction<in T1, TOut, in T2>(T1 arg1, out TOut outVal, T2 arg2);

        public static IReturnsThrows<TMock, TReturn> OutCallback<TMock, TReturn, T1, TOut, T2>
                                            (this ICallback<TMock, TReturn> mock, OutAction<T1, TOut, T2> action)
            where TMock : class
        {
            return OutCallbackInternal(mock, action);
        }

        private static IReturnsThrows<TMock, TReturn> OutCallbackInternal<TMock, TReturn>
                                                                  (ICallback<TMock, TReturn> mock, object action)
            where TMock : class
        {
            mock.GetType()
                .Assembly.GetType("Moq.MethodCall")
                .InvokeMember("SetCallbackWithArguments",
                               BindingFlags.InvokeMethod | BindingFlags.NonPublic | BindingFlags.Instance,
                               null, mock, new[] { action });

            return mock as IReturnsThrows<TMock, TReturn>;
        }
    }
}
