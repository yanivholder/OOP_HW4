package OOP.Solution;

import OOP.Provided.OOPResult;
import java.util.*;


public class OOPTestSummary {
    Map<OOPTestResult, Integer> map = new HashMap<OOPTestResult, Integer>();
//    map.put()
    private int n_success;
    private int n_fail;
    private int n_mismatch;
    private int n_error;

    OOPTestSummary (Map<String, OOPResult> testMap){
        for (OOPTestResult res : testMap.values()){
            switch (res)
        }
    }
    int getNumSuccesses(){

    }

    int getNumFailures(){

    }

    int getNumExceptionMismatches(){

    }

    int getNumErrors(){

    }
}
