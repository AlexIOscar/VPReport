package vpreportpkj.starter;

import vpreportpkj.core.ReportReader;
import vpreportpkj.core.SingleTuple;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EntryPoint {
    static int singleRBTime = 50;
    public static void main(String[] args) {
        String rep = getReport("C:\\IntellijProj\\VPReport\\src\\01_10_2021.txt");
        System.out.println(rep);
    }

    public static String getReport(String path){

        StringBuilder sb = new StringBuilder("Report generated at: " + new Date() + '\n');
        sb.append("Source: ").append(path).append('\n');

        List<String> strArr = ReportReader.getStrArr(path);
        assert strArr != null;

        List<SingleTuple> tuples = new ArrayList<>();
        for (String sign : strArr) {
            tuples.add(SingleTuple.generateTuple(sign));
        }

        long idleTime = 0;
        int carriageRollbacks = 0;

        for (int i = 0; i < tuples.size() - 1; i++) {
            Date completeThis = tuples.get(i).getCompleteTime();
            Date startNext = tuples.get(i + 1).getStartTime();
            if (!completeThis.after(startNext)) {
                long gap = ((startNext.getTime() - completeThis.getTime()) / 1000);
                if (gap >= singleRBTime) {
                    carriageRollbacks++;
                }
                idleTime = idleTime + ((startNext.getTime() - completeThis.getTime()) / 1000);
                /*
                System.out.println("time gap " + ((startNext.getTime() - completeThis.getTime()) / 1000) + " in report " +
                        "line " + (i + 1));
                 */
            }
        }
        System.out.println("____________________________________________________________________________");
        long uptime =
                (tuples.get((tuples.size() - 1)).getCompleteTime().getTime() - tuples.get(0).getStartTime().getTime()) / 1000;

        long operationTime = uptime - idleTime;
        long dealTime = operationTime + (long) carriageRollbacks * singleRBTime;

        sb.append("Total uptime, min: ").append(uptime / 60).append('\n');
        sb.append("Total idle, min: ").append(idleTime / 60).append('\n');
        sb.append("Operation time, min: ").append(operationTime / 60).append('\n');
        sb.append("Carriage rollbacks: ").append(carriageRollbacks).append('\n');
        sb.append("Deal time, min: ").append(dealTime / 60).append('\n');
        sb.append("Workload, %, by opTime: ").append(((double)operationTime / (double) uptime) * 100).append('\n');
        sb.append("Workload, %, by deal time: ").append(((double)dealTime / (double) uptime) * 100).append('\n');
        return  sb.toString();
    }
}
