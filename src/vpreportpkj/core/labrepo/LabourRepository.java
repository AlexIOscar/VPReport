package vpreportpkj.core.labrepo;

import vpreportpkj.core.SingleTuple;

public interface LabourRepository {
    void push(SingleTuple st);

    int chkTime(SingleTuple st);

    StringBuilder getStringBuilder();
}
