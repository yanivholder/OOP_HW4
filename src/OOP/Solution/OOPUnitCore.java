package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Solution.OOPTestSummary;

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

    public static OOPTestSummary runClass(Class<?> testClass, String tag) {

    }


}
