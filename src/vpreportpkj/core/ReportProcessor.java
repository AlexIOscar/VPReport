package vpreportpkj.core;

import vpreportpkj.core.labrepo.AdvancedRepo;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Если есть желание дальше развивать проект, то этот класс нужно как можно быстрее переработать под
 * instance-использование.
 * В своём утилитном виде он существует по историческим причинам, и это дает определенные проблемы.
 * А еще лучше сделать полностью новый, с более вменяемым API, не имеющим зависимости от порядка фильтраций, вызова
 * ресолвера времени, однотипно использующий флаги (например, обучения репозиториев), с выбором языка и тому подобное.
 * В этом же можно оставить только утилитные вещи, вроде вывода в файл.
 */
public class ReportProcessor {
    //характеристики, влияющие на оценочные показатели в отчете
    private static int singleRBTime = 50;
    private static int gapLimit = 400;
    private static int processingLimit = 600;
    private static int shiftDuration = 720;
    private static double whipLength = 12000;
    private static double kim = 0.85;
    private static boolean isDecrSuspPT = false;
    private static int decrSuspTTo = 50;
    private static int CRMMethodIndex = 0;
    public static boolean useFastRepo = false;
    public static boolean useRepository = false;
    private static boolean updateRepo = true;
    private static double filterFactor = 4;
    public static LabourEngine le;
    public static LabourEngine lec;

    /**
     * Выгружает отчет в файл
     *
     * @param outPath путь выгрузки
     * @param tuples  входной список кортежей, по которым необходимо сгенерировать отчет
     */
    public static void pushToFile(String outPath, List<SingleTuple> tuples) {
        List<String> rep = getReport(tuples, true, true);
        File outF = new File(outPath);

        try (FileWriter writer = new FileWriter(outF, false)) {
            for (String str : rep) {
                writer.write(str);
            }
            writer.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Выгружает отчет в файл (своего рода, перегруженная версия отчета для единого списка, но выведенная под
     * собственным именем из-за технических ограничений обобщений (generics erasure)).
     *
     * @param outPath    путь выгрузки
     * @param tuplesList входной список кортежей, разбитый на части, по которым необходимо сгенерировать отчет
     */
    public static void pushToFileForList(String outPath, List<List<SingleTuple>> tuplesList) {
        List<String> rep = getReportForList(tuplesList, false, false);
        File outF = new File(outPath);

        try (FileWriter writer = new FileWriter(outF, false)) {
            for (String str : rep) {
                writer.write(str);
            }
            writer.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    public static void savePcsCSV(String outPath, List<SingleTuple> tuples) {
        String str = generatePcsCSV(tuples);
        File outF = new File(outPath);

        try (FileWriter writer = new FileWriter(outF, false)) {
            writer.write(str);
            writer.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    public static void saveShiftsCSV(String outPath, List<List<SingleTuple>> shifts, double coeff, int shiftDuration) {
        String str = generateShiftsCSV(shifts, coeff, shiftDuration);
        File outF = new File(outPath);

        try (FileWriter writer = new FileWriter(outF, false)) {
            writer.write(str);
            writer.flush();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage());
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Получение отчета в виде списка строк. Каждая строка представляет собой определенную часть данных
     *
     * @param tuples  лист кортежей, для которых строится отчет
     * @param exData1 флаг включения расширенных данных - подозрительно высоких потерь времени между кортежами
     * @param exData2 флаг включения расширенных данных - подозрительно длительных случаев обработки одной детали
     * @return Отчет в виде списка строк
     */
    public static List<String> getReport(List<SingleTuple> tuples, boolean exData1, boolean exData2) {
        if (tuples.size() == 0) {
            throw new RuntimeException("Processing set is empty");
        }
        tuples = Util.sortTuples(tuples);
        List<String> report = new ArrayList<>();

        StringBuilder exBuilder = null;
        if (exData1) {
            exBuilder = new StringBuilder("Suspicious time wastes (more than " + gapLimit + " seconds):\n");
        }

        StringBuilder sb = new StringBuilder("Report generated at: " + new Date() + '\n');

        sb.append("Source: work period between ").append(tuples.get(0).getStartTime()).append(" and ")
                .append(tuples.get(tuples.size() - 1).getCompleteTime()).append('\n');

        sb.append("Mode: ");
        if (useFastRepo && isDecrSuspPT) {
            sb.append("smart full check-&-reducing activated\n");
        }
        if (isDecrSuspPT && !useFastRepo) {
            sb.append("brute reducing of suspicious work-times activated\n");
        }
        if (!isDecrSuspPT) {
            sb.append("none (work-times \"as-is\")\n");
        }

        long idleTime = 0;
        int carriageRollbacksByGaps = 0;

        for (int i = 0; i < tuples.size() - 1; i++) {
            Date completeThis = tuples.get(i).getCompleteTime();
            Date startNext = tuples.get(i + 1).getStartTime();
            if (!completeThis.after(startNext)) {
                long gap = ((startNext.getTime() - completeThis.getTime()) / 1000);
                if (gap >= singleRBTime) {
                    carriageRollbacksByGaps++;
                }
                if (exData1 && gap > gapLimit) {
                    exBuilder.append(gap).append(" seconds gap detected between ").append(completeThis)
                            .append(" and ").append(startNext).append('\n');
                }
                idleTime += gap;
            }
        }
        //printOverlayingInfo(tuples);

        //"аналитический" отчет
        long uptime =
                (tuples.get((tuples.size() - 1)).getCompleteTime().getTime() - tuples.get(0).getStartTime().getTime()) / 1000;

        //У этого куска сложная история, потому он так криво сделан. При случае отрефакторить
        long operationTime;
        if (isDecrSuspPT) {
            if (useFastRepo) {
                AtomicLong decreaseTime = new AtomicLong();
                tuples.forEach(t -> decreaseTime.addAndGet(t.getDuration() - le.chkTimeAdv(t, filterFactor, updateRepo)));
                tuples.stream().forEach(t -> lec.getRepository().chkTime(t));
                idleTime += decreaseTime.get();
            } else {
                long deltaTime;
                AtomicLong decreaseTime = new AtomicLong();
                long suspTimes = tuples.stream().filter(t -> t.getDuration() > processingLimit).peek(t -> decreaseTime.addAndGet(t.getDuration())).count();
                deltaTime = decreaseTime.get() - suspTimes * decrSuspTTo;
                idleTime += deltaTime;
            }
        }
        operationTime = uptime - idleTime;

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

        long dealTime;
        switch (CRMMethodIndex) {
            default:
            case 0:
                dealTime = operationTime + (long) (cuts - tuples.size()) * singleRBTime;
                break;
            case 1:
                dealTime = operationTime + (long) carriageRollbacksByGaps * singleRBTime;
                break;
            case 2:
                dealTime = operationTime + (long) (length / whipLength / kim) * singleRBTime;
        }

        sb.append("Total uptime, min: ").append(uptime / 60).append('\n')
                .append("Total idle, min: ").append(idleTime / 60).append('\n')
                .append("Operation time, min: ").append(operationTime / 60).append('\n')
                .append("Carriage rollbacks (estimated by time gaps): ").append(carriageRollbacksByGaps)
                .append(CRMMethodIndex == 1 ? " (active)" : "").append('\n')
                .append("Carriage rollbacks (estimated by total length): ").append((int) (length / whipLength / kim))
                .append(CRMMethodIndex == 2 ? " (active)" : "").append('\n')
                .append("Carriage rollbacks (estimated by extra cuts): ").append(cuts - tuples.size())
                .append(CRMMethodIndex == 0 ? " (active)" : "").append('\n')
                .append("Deal time, min: ").append(dealTime / 60).append('\n')
                .append("Workload, %, by period duration & opTime: ").append(((double) operationTime / (double) uptime) * 100).append('\n')
                .append("Workload, %, by period duration & deal time: ").append(((double) dealTime / (double) uptime) * 100).append('\n');

        if (uptime / 60.0 < shiftDuration * 1.1) {
            sb.append("Workload, %, by shift duration & deal time: ").append(((double) dealTime / (double) (shiftDuration * 60)) * 100).append('\n');
        }

        //вывод общих данных
        sb.append("____________________________Total amounts____________________________\n");
        sb.append("Total holes: ").append(holes).append('\n');
        sb.append("Total cuts: ").append(cuts).append('\n');
        sb.append("Total mass, kg: ").append(mass).append('\n');
        sb.append("Total length, m: ").append(length / 1000).append('\n');
        sb.append("Total pieces: ").append(tuples.size()).append("\n");
        sb.append(
                "-----------------------------------------------------------------------------------------------------------------------\n\n");

        report.add(sb.toString());

        if (exData1) {
            report.add(exBuilder.toString());
        }

        if (exData2) {
            report.add(getOverLabours(tuples));
        }

        return report;
    }

    /**
     * Версия получения отчета для списка кортежей, разбитого на части. Выведена под собственным именем (вместо
     * перегрузки) из-за технических ограничений обобщений (generics erasure)).
     *
     * @param shifts  разбитый на части список кортежей
     * @param exData1 флаг включения расширенных данных - подозрительно высоких потерь времени между кортежами
     * @param exData2 флаг включения расширенных данных - подозрительно длительных случаев обработки одной детали
     * @return Отчет в виде списка строк
     */
    public static List<String> getReportForList(List<List<SingleTuple>> shifts, boolean exData1, boolean exData2) {
        List<String> out = new ArrayList<>();
        for (List<SingleTuple> period : shifts) {
            List<String> report = getReport(period, exData1, exData2);
            out.addAll(report);
        }
        return out;
    }

    /**
     * Генерирует CSV-сводку для смен.
     * Для простоты внутренней реализации, операционное время получается сложением времен всех деталей, поэтому перед
     * использованием этого метода НЕОБХОДИМО вызвать Util.resolveTime на совокупности всех кортежей, которые будут
     * здесь обработаны, иначе будет получено завышенное операционное время (из-за наложений времен в сырых данных)
     *
     * @param shifts смены
     * @param coeff  коэффициент увеличения рабочего времени
     * @return semicolon-separated текст
     */
    public static String generateShiftsCSV(List<List<SingleTuple>> shifts, double coeff, int shiftDuration) {
        StringBuilder sb = new StringBuilder("Начало;Окончание;Масса,кг;Длина,мм;Деталей;Откатов каретки;" +
                "Отверстий;Машинное время,мин;Рабочее время,мин;Полное время,мин;Коэфф.загрузки(пв),%;Коэфф.загрузки" +
                "(св),%\n");
        shifts.forEach(sh -> {
            int shiftSize = sh.size();
            if (shiftSize == 0) {
                return;
            }
            Date startShift = sh.get(0).startTime;
            Date endShift = sh.get(shiftSize - 1).getCompleteTime();
            long uptime = (endShift.getTime() - startShift.getTime()) / 1000;
            AtomicLong extractedOpTime = new AtomicLong();
            sh.forEach(t -> extractedOpTime.addAndGet(((AdvancedRepo) lec.getRepository()).chkTime(t, false)));
            int rollbacks = sh.stream().mapToInt(t -> t.cuts).sum() - shiftSize;
            long opTime = extractedOpTime.get();
            long workTime = (long) (opTime + ((long) rollbacks * singleRBTime) * coeff);
            int totalHoles = sh.stream().mapToInt(t -> t.holeCount).sum();
            double totalLen = sh.stream().mapToDouble(t -> t.length).sum();
            double totalMass = sh.stream().mapToDouble(SingleTuple::getMass).sum();
            double workload1 = (double) workTime * 100 / uptime;
            double workload2 = (double) workTime * 100 / (shiftDuration * 60);

            sb.append(Util.getFormattedDate(startShift)).append(';')
                    .append(Util.getFormattedDate(endShift)).append(';')
                    .append(String.format("%.3f", totalMass)).append(';')
                    .append(String.format("%.3f", totalLen)).append(';')
                    .append(shiftSize).append(';')
                    .append(rollbacks).append(';')
                    .append(totalHoles).append(';')
                    .append(opTime / 60).append(';')
                    .append(workTime / 60).append(';')
                    .append(uptime / 60).append(';')
                    .append(String.format("%.3f", workload1)).append(';')
                    .append(String.format("%.3f", workload2)).append('\n');
        });
        return sb.toString();
    }

    /**
     * Получить сводку подозрительно высоких трудоемкостей в списке кортежей
     *
     * @param inputList Входной список кортежей
     * @return Строковое представление всех случаев, в которых детектировано подозрительно высокое время обработки
     * одной детали из входящего списка
     */
    public static String getOverLabours(List<SingleTuple> inputList) {
        StringBuilder sb = new StringBuilder("\nSuspicious labours (more than " + processingLimit + " seconds):\n");
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

    /**
     * Выводит в консоль случаи двойного и тройного перекрытия времени кортежами. Под перекрытием подразумевается
     * ситуация, когда в одно время производится более одной детали (детектируется одновременное производство двух и
     * трех деталей)
     *
     * @param tuples Входной лист кортежей, на которых выполняется тестирование перекрытия
     */
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

    private static String generatePcsCSV(List<SingleTuple> tuples) {
        StringBuilder sb = new StringBuilder();
        //sb.append("order;mark;position;start;complete;duration;mass;length;holes;cuts;roll\n");
        sb.append("Заказ;Марка;Позиция;Старт;Окончание;Время,сек;Время(фильтр),сек;Масса,кг;Длина,мм;Отверстий;Рубов;" +
                "Прокат\n");
        tuples.forEach(t -> sb.append(t.getOrder()).append(';')
                .append(t.getMark()).append(';')
                .append(t.getPosition()).append(';')
                .append(Util.getFormattedDate(t.getStartTime())).append(';')
                .append(Util.getFormattedDate(t.getCompleteTime())).append(';')
                .append(t.getDuration()).append(';')
                .append(((AdvancedRepo) lec.getRepository()).chkTime(t, false)).append(';')
                .append(t.mass).append(';')
                .append(t.length).append(';')
                .append(t.holeCount).append(';')
                .append(t.cuts).append(';')
                .append(t.roll).append('\n'));
        return sb.toString();
    }

    public static void setSingleRBTime(int singleRBTime) {
        ReportProcessor.singleRBTime = singleRBTime;
    }

    public static void setGapLimit(int gapLimit) {
        ReportProcessor.gapLimit = gapLimit;
    }

    public static void setProcessingLimit(int processingLimit) {
        ReportProcessor.processingLimit = processingLimit;
    }

    public static void setShiftDuration(int shiftDuration) {
        ReportProcessor.shiftDuration = shiftDuration;
    }

    public static void setWhipLength(double whipLength) {
        ReportProcessor.whipLength = whipLength;
    }

    public static void setKim(double kim) {
        ReportProcessor.kim = kim;
    }

    public static void setIsDecrSuspPT(boolean isDecrSuspPT) {
        ReportProcessor.isDecrSuspPT = isDecrSuspPT;
    }

    public static void setDecrSuspTTo(int decrSuspTTo) {
        ReportProcessor.decrSuspTTo = decrSuspTTo;
    }

    public static void setCRMMethodIndex(int CRMMethodIndex) {
        ReportProcessor.CRMMethodIndex = CRMMethodIndex;
    }

    public static void setUpdateRepo(boolean updateRepo) {
        ReportProcessor.updateRepo = updateRepo;
    }

    public static void setFilterFactor(double filterFactor) {
        ReportProcessor.filterFactor = filterFactor;
    }
}
