package OOP.Solution;

import OOP.Provided.OOPResult;
import java.util.*;

// TODO: test that impl is fine (different from f*)
public class OOPTestSummary {

    private int n_success = 0;
    private int n_fail = 0;
    private int n_mismatch = 0;
    private int n_error = 0;

    public OOPTestSummary(Map<String, OOPResult> testMap) {
        for (OOPResult res : testMap.values()) {
            switch (res.getResultType()) {
                case SUCCESS -> n_success++;
                case FAILURE -> n_fail++;
                case EXPECTED_EXCEPTION_MISMATCH -> n_mismatch++;
                case ERROR -> n_error++;
            }
        }
    }
    public int getNumSuccesses(){
        return n_success;
    }

    public int getNumFailures(){
        return n_fail;
    }

    public int getNumExceptionMismatches(){
        return n_mismatch;
    }

    public int getNumErrors(){
        return n_error;
    }
}
