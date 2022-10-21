package vpreportpkj.core.labrepo;

import vpreportpkj.core.SingleTuple;

public interface LabourRepository {

    int chkTime(SingleTuple st);

    StringBuilder getStringBuilder();
}
