package vpreportpkj.starter;

import vpreportpkj.core.ReportReader;
import vpreportpkj.core.SingleTuple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EntryPoint {
    static int singleRBTime = 50;
    static int gapLimit = 400;
    static int processingLimit = 600;

    public static void main(String[] args) {
        //String path = "C:\\IntellijProj\\VPReport\\src\\20_09_2022.txt";
        String path2 = "C:\\VPReportsTest\\26_09_2022.txt";
        List<String> rep = getReport(path2, true, true);
        System.out.println(rep.get(0));
        System.out.println(rep.get(1));
        System.out.println(rep.get(2));
    }

    public static void pushToFile(String rawFilePath) {
        List<String> rep = getReport(rawFilePath, true, true);
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

    /**
     * Метод сортирует кортежи по дате начала выполнения операции (содержащейся в кортеже). Предназначен для того,
     * чтоб исключить возможность перекрытия "потерянного" промежутка кортежами, отстоящими от текущего
     * дальше чем следующий (тупое объяснение, но лучшую формулировку сложно придумать)
     *
     * @param inputList входящий неотсортированный кортеж
     * @return исходящий отсортированный кортеж
     */
    private static List<SingleTuple> sortTuples(List<SingleTuple> inputList) {
        return inputList.stream()
                .sorted((t1, t2) -> (int) (t1.getStartTime().getTime() - t2.getStartTime().getTime()))
                .collect(Collectors.toList());
    }

    public static List<String> getReport(String path, boolean exData1, boolean exData2) {

        List<String> report = new ArrayList<>();

        StringBuilder exBuilder = null;
        if (exData1) {
            exBuilder = new StringBuilder("Suspicious time wastes (more than " + gapLimit + " seconds):\n");
        }

        StringBuilder sb = new StringBuilder("Report generated at: " + new Date() + '\n');
        sb.append("Source: ").append(path).append('\n');

        List<String> strArr = ReportReader.getStrArr(path);
        assert strArr != null;

        List<SingleTuple> tuples = new ArrayList<>();
        for (String sign : strArr) {
            tuples.add(SingleTuple.generateTuple(sign));
        }

        tuples = sortTuples(tuples);

        //System.out.println(tuples);

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

        sb.append("Total uptime, min: ").append(uptime / 60).append('\n')
                .append("Total idle, min: ").append(idleTime / 60).append('\n')
                .append("Operation time, min: ").append(operationTime / 60).append('\n')
                .append("Carriage rollbacks (estimated): ").append(carriageRollbacks).append('\n')
                .append("Deal time, min: ").append(dealTime / 60).append('\n')
                .append("Workload, %, by opTime: ").append(((double) operationTime / (double) uptime) * 100).append('\n')
                .append("Workload, %, by deal time: ").append(((double) dealTime / (double) uptime) * 100).append('\n');


        sb.append("____________________________________________________________________________\n");
        //подсчет общих данных
        int holes = 0;
        double mass = 0;
        for (SingleTuple tuple : tuples) {
            holes = holes + tuple.getHoleCount();
            mass = mass + tuple.getMass();
        }
        sb.append("Total holes: ").append(holes).append('\n');
        sb.append("Total mass, kg: ").append(mass).append('\n');
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
