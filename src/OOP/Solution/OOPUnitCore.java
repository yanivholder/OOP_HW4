package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;
import OOP.Solution.OOPTestClass;
import OOP.Solution.OOPTestSummary;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public class OOPUnitCore {

    public static void assertEquals(Object expected, Object actual) throws OOPAssertionFailure {
        if((expected != null && actual == null)
                || (expected == null && actual != null)
                || (expected != null && actual != null && !actual.equals(expected))) {
            throw new OOPAssertionFailure();
        }
    }

    public static void fail() throws OOPAssertionFailure {
        throw new OOPAssertionFailure();
    }

    public static OOPTestSummary runClass(Class<?> testClass) {
        return innerRunClass(testClass, null);
    }

    public static OOPTestSummary runClass(Class<?> testClass, String tag) {
        if (tag == null) throw new IllegalArgumentException();
        return innerRunClass(testClass, tag);
    }

    private static OOPTestSummary innerRunClass(Class<?> testClass, String tag) throws IllegalArgumentException {
        if(testClass == null || !testClass.isAnnotationPresent(OOPTestClass.class)) {
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
                        m.invoke(newTestClass);
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

    // A minimal wrapper class for values, because `Collectors.toMap` doesn't
    // support null values, so we have to wrap them.
    private static class Value {
        public Object x;
        public Value(Object x) {
            this.x = x;
        }
    }

    // Run all OOPBefore all OOPAfter methods relevant to testMethod.
    // @param isBefore: if true, run OOPBefore methods. Otherwise, run OOPAfter
    // methods.
    // @return success state: null if all methods ran successfully, otherwise the
    // class of the exception that was raised.
    private static Class<? extends Exception> runWithBackup(Method testMethod, Object instance, boolean isBefore) {
        Stream<Method> method_stream = null;
        if (isBefore) {
            method_stream = getMethodsWithAnnotation(instance.getClass(), OOPBefore.class, true)
                    .filter(m -> Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(testMethod.getName()));
        } else {
            method_stream = getMethodsWithAnnotation(instance.getClass(), OOPAfter.class, false)
                    .filter(m -> Arrays.asList(m.getAnnotation(OOPAfter.class).value()).contains(testMethod.getName()));
        }
        List<Method> methods = method_stream.collect(Collectors.toList());

        // execute all before_methods, with short-circuit if any of them cause
        // an error (short-circuit + lambdas with side effects => a stream isn't
        // appropriate here)

        // `Collectors.toMap` doesn't allow null values, but fields can have a
        // null value. We solve the contradiction by wrapping each value in a
        // Value object. cloneField never returns null, so we are safe.
        Map<Field, Value> backup_date = Arrays.stream(instance.getClass().getDeclaredFields())
                .collect(Collectors.toMap(field -> field, field -> cloneField(instance, field)));
        for (Method m : methods) {
            try {
                m.setAccessible(true);
                m.invoke(instance);
            } catch (Exception e) {
                restore(instance, backup_date);
                return e.getClass();
            }
        }

        return null;
    }

    // @return a sorted stream of all methods of testClass that are annotated by
    // the given annotation. The result is sorted by order of inheritance: from
    // methods declared in testClass to methods declared upward in the
    // inheritance-chain of testClass. The result includes both public and
    // non-public methods. The result filters out ancestor methods that were
    // overridden by a child, keeping only the overriding method.
    // @param topDown: if topDown is True, then the result is sorted in the
    // reverse order (i.e., from ancestor to child).
    private static Stream<Method> getMethodsWithAnnotation(Class<?> testClass,
                                                           Class<? extends Annotation> annotationClass, boolean topDown) {
        //Stream.Builder<Method> methods = Stream.builder();
        List<Class<?>> ancestors = sortedAncestors(testClass);
        if (topDown) {
            Collections.reverse(ancestors);
        }
        List<Method> methods = new ArrayList<>();
        ancestors.stream()
                .flatMap(cls -> Arrays.stream(cls.getDeclaredMethods()))
                .filter(m -> m.isAnnotationPresent(annotationClass))
                .forEach(method -> {
                    if (topDown) {
                        methods.removeIf(m -> method.getName().equals(m.getName()));
                        methods.add(method);
                    } else if (methods.stream().filter(m -> method.getName().equals(m.getName())).findAny().isEmpty()) {
                        methods.add(method);
                    }
                });
        return methods.stream();
    }

    // @return a list of all cls ancestors, sorted from cls upwards, excluding
    // the null at the end.
    private static List<Class<?>> sortedAncestors(Class<?> cls) {
        List<Class<?>> ancestors = new ArrayList<>();
        for (; cls != null; cls = cls.getSuperclass()) {
            ancestors.add(cls);
        }
        return ancestors;
    }

    // @return a clone of the specified field of obj
    private static Value cloneField(Object obj, Field field) {
        Object value = null;
        try {
            field.setAccessible(true);
            value = field.get(obj);
        } catch (IllegalAccessException e) {
            // should never arrive here
            // e.printStackTrace();
        }

        if (value == null) {
            return new Value(null);
        }

        Class<?> cls = value.getClass();
        if (Cloneable.class.isInstance(value)) {
            try {
                // can't use cls.getMethod("clone"), because it ignores
                // protected methods - while clone can be protected. No need to
                // worry about clone methods with less visibility than protected
                // (i.e., private clone), because we already checked that cls
                // implements Cloneable interface (which enforces a visibility
                // of at least protected).
                Method clone = sortedAncestors(cls).stream().map(ancestor -> {
                            try {
                                return ancestor.getDeclaredMethod("clone");
                            } catch (NoSuchMethodException e) {
                                return null;
                            }
                        })
                        .dropWhile(method -> method == null)
                        .findFirst().orElseThrow();
                clone.setAccessible(true);
                value = clone.invoke(value);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchElementException e) {
                // should never arrive here
                // e.printStackTrace();
            }
        }
        else{
            try {
                Constructor<?> ctor = cls.getDeclaredConstructor(cls);
                ctor.setAccessible(true);
                value = ctor.newInstance(value);
            } catch (NoSuchMethodException e) {
                // do nothing ( implicit value = field.get(obj) )
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return new Value(value);
    }


    // set the specified field of an object to the specified value. Assumes
    // `value` is a legal value for the field.
    private static void setField(Object obj, Field field, Value value) {
        field.setAccessible(true);
        try {
            field.set(obj, value.x);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // should never arrive here
            // e.printStackTrace();
        }
    }

    // Set all the fields of `obj` according to their backup in `backup`
    private static void restore(Object obj, Map<Field, Value> backup) {
        backup.entrySet().stream().forEach(entry -> setField(obj, entry.getKey(), entry.getValue()));
    }


}
