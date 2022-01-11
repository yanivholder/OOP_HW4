package OOP.Solution;

import java.util.LinkedList;

import OOP.Provided.OOPExpectedException;

public class OOPExpectedExceptionImpl implements OOPExpectedException {
    private Class<? extends Exception> expected_exception = null;
    private LinkedList<String> messages = new LinkedList<>();

    @Override
    public Class<? extends Exception> getExpectedException() {
        return this.expected_exception;
    }

    @Override
    public OOPExpectedException expect(Class<? extends Exception> expected) {
        this.expected_exception = expected;
        return this;
    }

    @Override
    public OOPExpectedException expectMessage(String msg) {
        this.messages.add(msg);
        return this;
    }

    @Override
    public boolean assertExpected(Exception e) {
        if(this.expected_exception == null && e == null) {
            return true;
        }
//        if(this.expected_exception.isInstance(e) && this.messages)
        return false;
    }

    public static OOPExpectedException none() {
        return new OOPExpectedExceptionImpl();
    }
}
