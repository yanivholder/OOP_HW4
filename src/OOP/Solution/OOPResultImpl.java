package OOP.Solution;

import OOP.Provided.OOPResult;

public class OOPResultImpl implements OOPResult {

    private OOPTestResult result_type;
    private String message;

    public OOPResultImpl(OOPTestResult res_type, String msg) {
        this.result_type = res_type;
        this.message = msg;
    }

    @Override
    public OOPTestResult getResultType() {
        return this.result_type;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null) && (obj.getClass() == this.getClass()) &&
                ((OOPResultImpl) obj).getMessage().equals(this.getMessage()) &&
                (this.getResultType() == ((OOPResultImpl) obj).getResultType());
    }
}
