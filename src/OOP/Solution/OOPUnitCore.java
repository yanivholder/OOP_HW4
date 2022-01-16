package OOP.Solution;

import OOP.Provided.OOPAssertionFailure;
import OOP.Provided.OOPExceptionMismatchError;
import OOP.Provided.OOPExpectedException;
import OOP.Provided.OOPResult;

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
            Constructor<?> mt = Arrays.stream(testClass.getDeclaredConstructors())
                    .filter(ctor -> ctor.getParameterCount() == 0)
                    .findFirst().get();
            mt.setAccessible(true);
            return mt.newInstance();

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
        Class<? extends Exception> e = run_and_backup(m, obj, true);
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
        e = run_and_backup(m, obj, false);
        if(e != null) {
            res = new OOPResultImpl(OOPResult.OOPTestResult.ERROR, e.getName());
        }
        return res;
    }

    private static class Val_wrap {
        public Object x;
        public Val_wrap(Object x) {
            this.x = x;
        }
    }

    private static Class<? extends Exception> run_and_backup(Method testMethod, Object obj, boolean isBefore) {
        Stream<Method> method_stream = null;
        Class<?> cls = isBefore ? OOPBefore.class : OOPAfter.class;
        if (isBefore){
            method_stream = getMethodStreamWithGivenAnnotation(obj.getClass(), OOPBefore.class, isBefore)
                    .filter(m -> Arrays.asList(m.getAnnotation(OOPBefore.class).value()).contains(testMethod.getName()));
        }else{
            method_stream = getMethodStreamWithGivenAnnotation(obj.getClass(), OOPAfter.class, isBefore)
                    .filter(m -> Arrays.asList(m.getAnnotation(OOPAfter.class).value()).contains(testMethod.getName()));
        }



        List<Method> methods = method_stream.collect(Collectors.toList());

        Map<Field, Val_wrap> backup_date = Arrays.stream(obj.getClass().getDeclaredFields())
                .collect(Collectors.toMap(field -> field, field -> clone_orCopyConstr_orSaveVal(obj, field)));
        for (Method m : methods) {
            try {
                m.setAccessible(true);
                m.invoke(obj);
            } catch (Exception e) {
                restore_obj(obj, backup_date);
                return e.getClass();
            }
        }

        return null;
    }


    // @return a clone of the specified field of obj
    private static Val_wrap clone_orCopyConstr_orSaveVal(Object obj, Field field) {
        Object value = null;
        try {
            field.setAccessible(true);
            value = field.get(obj);
        } catch (IllegalAccessException e) {
            // should never arrive here
            // e.printStackTrace();
        }

        if (value == null) {
            return new Val_wrap(null);
        }

        Class<?> cls = value.getClass();
        if (Cloneable.class.isInstance(value)) {
            try {
                List<Class<?>> ancestors = new ArrayList<>();
                for (; cls != null; cls = cls.getSuperclass()) {
                    ancestors.add(cls);
                }
                Method clone = ancestors.stream().map(ancestor -> {
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
        return new Val_wrap(value);
    }


    private static void restore_field(Object obj, Field field, Val_wrap value) {
        field.setAccessible(true);
        try {
            field.set(obj, value.x);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void restore_obj(Object obj, Map<Field, Val_wrap> backup) {
        backup.entrySet().stream().forEach(entry -> restore_field(obj, entry.getKey(), entry.getValue()));
    }

}
