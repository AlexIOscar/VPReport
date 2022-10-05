package vpreportpkj.starter;

import vpreportpkj.core.SingleTuple;
import vpreportpkj.core.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EntryPoint {
    //характеристики, влияющие на оценочные показатели в отчете
    static int singleRBTime = 50;
    static int gapLimit = 400;
    static int processingLimit = 600;
    static int shiftDuration = 720;
    static double whipLength = 12000;
    static double kim = 0.85;

    public static void main(String[] args) {
        //String path2 = "C:\\IntellijProj\\VPReport\\src\\20_09_2022.txt";
        String path = "C:\\VPReportsTest\\29_09_2022_morn.txt";

        /*
        List<SingleTuple> tuples = Util.getTuplesList(path);
        List<List<SingleTuple>> periods = Util.splitPeriods(tuples, new int[]{8, 20});

        List<String> rep = getReportForList(periods, true, true);
        rep.forEach(System.out::println);
         */

        //склеиваем все отчеты в папке в единый пул
        List<SingleTuple> commonList = Util.getCommonList("C:\\VPReportsTest");
        //общий отчет
        List<String> rep2 = getReport(commonList, false, false);
        List<List<SingleTuple>> periods = Util.splitPeriods(commonList, new int[]{8, 20}, new int[]{0, 0});
        //посменный отчет
        List<String> rep3 = getReportForList(periods, false, false);
        rep2.forEach(System.out::println);
        System.out.println("\n\n____________Посменно:_____________");
        rep3.forEach(System.out::println);
    }

    public static void pushToFile(String rawFilePath, List<SingleTuple> tuples) {
        List<String> rep = getReport(tuples, true, true);
        File outF = new File(rawFilePath.replaceAll(".txt", "") + "_report.txt");

        try (FileWriter writer = new FileWriter(outF, false)) {
            writer.write(rep.get(0));
            writer.write(rep.get(1));
            writer.write(rep.get(2));
            writer.flush();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static List<String> getReport(List<SingleTuple> tuples, boolean exData1, boolean exData2) {

        List<String> report = new ArrayList<>();

        StringBuilder exBuilder = null;
        if (exData1) {
            exBuilder = new StringBuilder("Suspicious time wastes (more than " + gapLimit + " seconds):\n");
        }

        StringBuilder sb = new StringBuilder("Report generated at: " + new Date() + '\n');

        sb.append("Source: work period between ").append(tuples.get(0).getStartTime()).append(" and ")
                .append(tuples.get(tuples.size() - 1).getCompleteTime()).append('\n');

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
                if (exData1 && gap > gapLimit) {
                    exBuilder.append(gap).append(" seconds gap detected between ").append(completeThis)
                            .append(" and ").append(startNext).append('\n');
                }

                idleTime = idleTime + ((startNext.getTime() - completeThis.getTime()) / 1000);

                /*
                System.out.println("time gap " + ((startNext.getTime() - completeThis.getTime()) / 1000) + " in report " +
                        "line " + (i + 1));
                 */

            }
        }
        //printOverlayingInfo(tuples);

        //"аналитический" отчет
        long uptime =
                (tuples.get((tuples.size() - 1)).getCompleteTime().getTime() - tuples.get(0).getStartTime().getTime()) / 1000;

        long operationTime = uptime - idleTime;
        long dealTime = operationTime + (long) carriageRollbacks * singleRBTime;

        //подсчет общих данных
        int holes = 0;
        double mass = 0;
        double length = 0;
        int cuts = 0;
        for (SingleTuple tuple : tuples) {
            holes = holes + tuple.getHoleCount();
            mass = mass + tuple.getMass();
            length = length + tuple.getLength();
            cuts = cuts + tuple.getCuts();
        }

        sb.append("Total uptime, min: ").append(uptime / 60).append('\n')
                .append("Total idle, min: ").append(idleTime / 60).append('\n')
                .append("Operation time, min: ").append(operationTime / 60).append('\n')
                .append("Carriage rollbacks (estimated by time gaps): ").append(carriageRollbacks).append('\n')
                .append("Carriage rollbacks (estimated by total length): ").append((int) (length / whipLength / kim)).append('\n')
                .append("Carriage rollbacks (estimated by extra cuts): ").append(cuts - tuples.size()).append('\n')
                .append("Deal time, min: ").append(dealTime / 60).append('\n')
                .append("Workload, %, by period duration & opTime: ").append(((double) operationTime / (double) uptime) * 100).append('\n')
                .append("Workload, %, by period duration & deal time: ").append(((double) dealTime / (double) uptime) * 100).append('\n')
                .append("Workload, %, by shift duration & deal time: ").append(((double) dealTime / (double) (shiftDuration * 60)) * 100).append('\n');

        //вывод общих данных
        sb.append("____________________________Total amounts____________________________\n");
        sb.append("Total holes: ").append(holes).append('\n');
        sb.append("Total cuts: ").append(cuts).append('\n');
        sb.append("Total mass, kg: ").append(mass).append('\n');
        sb.append("Total length, mm: ").append(length).append('\n');
        sb.append("Total pieces: ").append(tuples.size()).append('\n');

        report.add(sb.toString());

        if (exData1) {
            report.add(exBuilder.toString());
        }

        if (exData2) {
            report.add(getOverLabours(tuples));
        }

        return report;
    }

    public static List<String> getReportForList(List<List<SingleTuple>> shifts, boolean exData1, boolean exData2) {
        List<String> out = new ArrayList<>();
        for (List<SingleTuple> period : shifts) {
            List<String> report = getReport(period, exData1, exData2);
            out.addAll(report);
        }
        return out;
    }

    public static String getOverLabours(List<SingleTuple> inputList) {
        StringBuilder sb = new StringBuilder("Suspicious labours (more than " + processingLimit + " seconds):\n");
        inputList.stream().filter(l -> l.getDuration() > processingLimit).forEach(l -> sb.append(l.getDuration())
                .append(" processing duration detected for ")
                .append(l.getOrder())
                .append(' ')
                .append(l.getMark())
                .append(' ')
                .append(l.getPosition())
                .append(" at ")
                .append(l.getCompleteTime())
                .append('\n'));
        return sb.toString();
    }

    private static void printOverlayingInfo(List<SingleTuple> tuples) {
        //double overlaying
        for (int i = 0; i < tuples.size() - 2; i++) {
            Date completeThis = tuples.get(i).getCompleteTime();
            Date startNextNext = tuples.get(i + 2).getStartTime();
            if (completeThis.after(startNextNext)) {
                System.out.println("double overlaying detected between lines " + (i + 1) + " and " + (i + 3));
            }
        }

        //triple overlaying
        boolean detected = false;
        for (int i = 0; i < tuples.size() - 3; i++) {
            Date completeThis = tuples.get(i).getCompleteTime();
            Date startNNN = tuples.get(i + 3).getStartTime();
            if (completeThis.after(startNNN)) {
                System.out.println("triple overlaying detected between lines " + (i + 1) + " and " + (i + 4));
                detected = true;
            }
        }
        if (!detected) {
            System.out.println("No cases of triple overlaying were detected");
        }
    }
}
