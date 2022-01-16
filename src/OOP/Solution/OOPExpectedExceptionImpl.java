package OOP.Solution;

import java.util.LinkedList;

import OOP.Provided.OOPExpectedException;

public class OOPExpectedExceptionImpl implements OOPExpectedException {
    private Class<? extends Exception> expected_exception;
    private LinkedList<String> messages;

    public OOPExpectedExceptionImpl() {
        this.expected_exception = null;
        this.messages = new LinkedList<>();
    }

    // TODO: check maybe it should return OOPResult
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
        if(this.expected_exception.isInstance(e)) {
            if(this.messages.size() == 0 && e.getMessage() != null
                    ||this.messages.size() > 0 && e.getMessage() == null) {
                return false;
            }
            if(this.messages != null && e.getMessage() != null
                    && (!this.messages.stream().allMatch(m -> e.getMessage().contains(m)))) {
                return false;
            }
        }
        else return false;
        return true;
    }

    public static OOPExpectedException none() {
        return new OOPExpectedExceptionImpl();
    }
}
