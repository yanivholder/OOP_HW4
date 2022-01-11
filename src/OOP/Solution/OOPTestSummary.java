package OOP.Solution;

import OOP.Provided.OOPResult;
import java.util.*;


public class OOPTestSummary {
//    Map<OOPResult.OOPTestResult, Integer> map = new HashMap<OOPResult.OOPTestResult, Integer>();
//    map.put(OOPResult.OOPTestResult.SUCCESS, 0);
//    map.put(OOPResult.OOPTestResult.SUCCESS, 0);
//    map.put(OOPResult.OOPTestResult.SUCCESS, 0);
//    map.put(OOPResult.OOPTestResult.SUCCESS, 0);

    private int n_success = 0;
    private int n_fail = 0;
    private int n_mismatch = 0;
    private int n_error = 0;

    OOPTestSummary (Map<String, OOPResult> testMap){
        for (OOPResult res : testMap.values()){
            switch (res.getResultType()){
                case SUCCESS -> n_success++;
                case FAILURE -> n_fail++;
                case EXPECTED_EXCEPTION_MISMATCH -> n_mismatch++;
                case ERROR -> n_error++;
            }
        }
    }
    int getNumSuccesses(){
        return n_success;
    }

    int getNumFailures(){
        return n_fail;
    }

    int getNumExceptionMismatches(){
        return n_mismatch;
    }

    int getNumErrors(){
        return n_error;
    }
}
