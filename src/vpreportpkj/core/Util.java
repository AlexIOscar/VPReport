package vpreportpkj.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    public static List<List<SingleTuple>> splitPeriods(List<SingleTuple> input, int[] splitHours,
                                                       int[] splitMinutes) {
        List<List<SingleTuple>> output = new ArrayList<>();
        if (input.size() == 0) {
            return output;
        }
        input = sortTuples(input);
        Date first = input.get(0).startTime;
        Date last = input.get(input.size() - 1).completeTime;

        List<Date> splitPoints = getSplitMap(splitHours, splitMinutes, first, last);

        int pointer = 0;
        List<SingleTuple> buffer = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (input.get(i).getStartTime().before(splitPoints.get(pointer))) {
                buffer.add(input.get(i));
            } else {
                //сливаем буфер
                output.add(buffer);
                //заводим под него новый список
                buffer = new ArrayList<>();
                //двигаем пойнтер границы
                pointer++;
                //нужно, чтобы повторно пройтись по той детали, которая перешла "границу"
                i--;
            }
        }
        //сливаем накопленный буфер, поскольку в цикле никогда не наступит условие слива последнего буфера
        output.add(buffer);
        //clearing empty shifts
        output = output.stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());

        return output;
    }

    private static List<Date> getSplitMap(int[] splitHours, int[] splitMinutes, Date first, Date last) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(first);
        int stYear = cal.get(Calendar.YEAR);
        int stMonth = cal.get(Calendar.MONTH);
        int stDay = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(last);
        int enYear = cal.get(Calendar.YEAR);
        int enMonth = cal.get(Calendar.MONTH);
        int enDay = cal.get(Calendar.DAY_OF_MONTH);

        List<Date> splitPoints = new ArrayList<>();

        Calendar stepper = Calendar.getInstance();
        //get end-of-cicle-date
        stepper.set(enYear, enMonth, enDay + 1);
        Date stopTime = stepper.getTime();
        stepper.set(stYear, stMonth, stDay);
        for (int y = stYear; y <= enYear; y++) {
            do {
                int mon = stepper.get(Calendar.MONTH);
                int d = stepper.get(Calendar.DAY_OF_MONTH);
                for (int pointer = 0; pointer <= splitHours.length - 1 && pointer <= splitMinutes.length - 1; pointer++) {
                    cal.set(y, mon, d, splitHours[pointer], splitMinutes[pointer], 0);
                    splitPoints.add(cal.getTime());
                }
                //86400000 millis in day
                long time = stepper.getTime().getTime() + 86_400_000;
                //step to next day midnight
                stepper.setTime(new Date(time));
            } while (stepper.getTime().before(stopTime));
        }
        //far-far future comes...
        splitPoints.add(new Date(Long.MAX_VALUE));
        return splitPoints;
    }

    public static List<List<SingleTuple>> splitPeriods(List<SingleTuple> input, int[] splitHours) {
        int[] spMin = new int[splitHours.length];
        return splitPeriods(input, splitHours, spMin);
    }

    /**
     * Метод сортирует кортежи по дате начала выполнения операции (содержащейся в кортеже). Предназначен для того,
     * чтоб исключить возможность перекрытия "потерянного" промежутка кортежами, отстоящими от текущего
     * дальше чем следующий (тупое объяснение, но лучшую формулировку сложно придумать)
     *
     * @param inputList входящий неотсортированный кортеж
     * @return исходящий отсортированный кортеж
     */
    public static List<SingleTuple> sortTuples(List<SingleTuple> inputList) {
        return inputList.stream()
                .sorted((t1, t2) -> (int) (t1.getStartTime().getTime() - t2.getStartTime().getTime()))
                .collect(Collectors.toList());
    }

    public static List<SingleTuple> getTuplesList(String path) {
        List<String> strArr = ReportReader.getStrArr(path);
        assert strArr != null;

        List<SingleTuple> tuples = new ArrayList<>();
        for (String sign : strArr) {
            SingleTuple tuple = SingleTuple.generateTuple(sign);
            tuples.add(tuple);
        }

        tuples = Util.sortTuples(tuples);
        return tuples;
    }

    public static List<SingleTuple> getCommonList(String path) {

        List<Path> paths = null;
        //File list creation
        try (Stream<Path> pstr = Files.walk(Paths.get(path))) {
            paths = pstr.filter(Files::isRegularFile)
                    .filter(f -> f.toString().contains(".txt"))
                    .collect(Collectors.toList());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        List<SingleTuple> outList = new ArrayList<>();
        assert paths != null;
        paths.forEach(p -> outList.addAll(getTuplesList(p.toString())));
        return outList;
    }
}