package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPResult;
import OOP.Solution.OOPTestClass;
import OOP.Solution.OOPTestSummary;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OOPUnitCore {

    public static void assertEquals(Object expected, Object actual) throws OOPAssertionFailure {
        if(!actual.equals(expected)) {
            throw new OOPAssertionFailure();
        }
    }

    public static void fail() throws OOPAssertionFailure {
        throw new OOPAssertionFailure();
    }

    public static OOPTestSummary runClass(Class<?> testClass) {
        return runClass(testClass, null);
    }

    public static OOPTestSummary runClass(Class<?> testClass, String tag) throws IllegalArgumentException {
        // TODO: check if the exception should be thrown when tag == null
        if(testClass == null || testClass.isAnnotationPresent(OOPTestClass.class)) {
            throw new IllegalArgumentException();
        }

        // Step 1
        Object newTestClass = CreateNewTestClass(testClass);

        // Step 2
        setup(testClass, newTestClass);

        // Steps 3-5
        Map<String, OOPResult> testMap = runAllTests(testClass, tag, newTestClass);

        // Step 6
        return new OOPTestSummary(testMap);
    }

    private static Object CreateNewTestClass(Class<?> testClass) {
        // TODO: implement
        return new Object();
    }

    private static void setup(Class<?> testClass, Object newTestClass) {
        // TODO: implement
    }

    private static Map<String, OOPResult> runAllTests(Class<?> testClass, String tag, Object newTestClass) {
        // TODO: implement
        return new HashMap<String, OOPResult>();
    }

    // Run all OOPBefore all OOPAfter methods relevant to testMethod.
    // @param isBefore: if true, run OOPBefore methods. Otherwise, run OOPAfter
    // methods.
    // @return success state: null if all methods ran succefully, otherwise the
    // class of the exception that was raised.
    private static Class<? extends Exception> runWithBackup(Method testMethod, Object instance, boolean isBefore) {
        
        return null;
    }


}
