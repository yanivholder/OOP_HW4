package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;
import OOP.Solution.OOPTestClass;
import OOP.Solution.OOPTestSummary;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
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
        try {
            return Arrays.stream(testClass.getDeclaredConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 0)
                    .findFirst().get().newInstance();

        }
        catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    private static void setup(Class<?> testClass, Object newTestClass) {
        getMethodStreamWithGivenAnnotation(testClass, OOPSetup.class, true)
                .forEach(m -> {
                    m.setAccessible(true);
                    try {
                        m.invoke(true);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static Stream<Method> getMethodStreamWithGivenAnnotation(
            Class<?> testClass, Class<? extends Annotation> annotation,
            boolean isTopBottom) {
        List<Class<?>> parents = new ArrayList<>();
        List<Method> methods = new ArrayList<>();
        while(testClass != null) {
            parents.add(testClass);
            testClass = testClass.getSuperclass();
        }
        if(isTopBottom) {
            Collections.reverse(parents);
        }
        parents.stream()
                .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(annotation))
                .forEach(mtd -> {
                    if(isTopBottom) {
                        methods.removeIf(m -> mtd.getName().equals(m.getName()));
                        methods.add(mtd);
                    } else if(methods.stream()
                            .filter(m -> mtd.getName().equals(m.getName())).findAny().isEmpty()) {
                            //.anyMatch(m -> mtd.getName().equals(m.getName()))) {
                        methods.add(mtd);
                    }
                });
        return methods.stream();
    }

    private static Map<String, OOPResult> runAllTests(Class<?> testClass, String tag, Object newTestClass) {
        Map<String, OOPResult> res = new TreeMap<>();
        Stream<Method> methods = Arrays.stream(testClass.getMethods())
                .filter(m -> m.isAnnotationPresent(OOPTest.class));
        if(tag != null) {
            methods = methods.filter(m -> tag.equals(m.getAnnotation(OOPTest.class).tag()));
        }
        if(testClass.getAnnotation(OOPTestClass.class).value().equals(OOPTestClass.OOPTestClassType.ORDERED)) {
            methods = methods.sorted(Comparator.comparingInt(m -> m.getAnnotation(OOPTest.class).order()));
        }
        methods.forEach(m -> res.put(m.getName(), testRun(m, newTestClass)));
        return res;
    }

    private static OOPResult testRun(Method m, Object obj) {
        Class<? extends Exception> e = runWithBackup(m, obj, true);
        if(e != null) return new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getName());

        OOPResult res = null;
        Field fld = Arrays.stream(obj.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(OOPExceptionRule.class))
                .findFirst()
                .orElse(null);
        if(fld != null) {
            fld.setAccessible(true);
            try {
                fld.set(obj, OOPExpectedExceptionImpl.none());
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
        OOPExpectedException expected = null;
        try {
            m.setAccessible(true);
            m.invoke(obj);
            if(fld == null) {
                expected = OOPExpectedExceptionImpl.none();
            } else {
                expected = ((OOPExpectedException) fld.get(obj));
            }
            if(expected.getExpectedException() != null) {
                res = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, expected.getExpectedException().getName());
            }
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        } catch (InvocationTargetException ex) {
            if(fld == null) {
                expected = OOPExpectedExceptionImpl.none();
            } else {
                try {
                    expected = ((OOPExpectedException) fld.get(obj));
                } catch (IllegalAccessException exc) {
                    exc.printStackTrace();
                }
            }
            if(OOPAssertionFailure.class.isInstance(ex.getCause())) {
                res = new OOPResultImpl(OOPResult.OOPTestResult.FAILURE, ex.getCause().getMessage());
            } else {
                Exception exp = (Exception) ex.getCause();
                if(expected.getExpectedException() == null) {
                    res = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, exp.getClass().getName());
                } else if(!expected.assertExpected(exp)) {
                    res = new OOPResultImpl(OOPResult.OOPTestResult.EXPECTED_EXCEPTION_MISMATCH,
                            new OOPExceptionMismatchError(expected.getExpectedException(),
                                    exp.getClass()).getMessage());
                }
            }
        }
        if(res == null) {
            res = new OOPResultImpl(OOPResult.OOPTestResult.SUCCESS, null);
        }
        e = runWithBackup(m, obj, false);
        if(e != null) {
            res = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getName());
        }
        return res;
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
