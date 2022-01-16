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

    protected boolean eq(Object o) {
        if (!(o instanceof OOPResultImpl)) return false;
        if((this.getMessage() == null && ((OOPResultImpl)o).getMessage() != null)
                || (this.getMessage() != null && ((OOPResultImpl)o).getMessage() == null)) {
            return false;
        }
        if((this.getMessage() != null && ((OOPResultImpl)o).getMessage() != null)
                && !this.getMessage().equals(((OOPResultImpl)o).getMessage())) {
            return false;
        }

        if((this.getResultType() == null && ((OOPResultImpl)o).getResultType() != null)
                || (this.getResultType() != null && ((OOPResultImpl)o).getResultType() == null)) {
            return false;
        }
        if((this.getResultType() != null && ((OOPResultImpl)o).getResultType() != null)
                && !this.getResultType().equals(((OOPResultImpl)o).getResultType())) {
            return false;
        }
        return true;
    }
    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        return (this.eq(o) && ((OOPResultImpl)o).eq(this));
    }
}
