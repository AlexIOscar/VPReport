package vpreportpkj.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
    public static List<List<SingleTuple>> splitPeriods(List<SingleTuple> input, int[] splitHours,
                                                       int[] splitMinutes) {

        input = sortTuples(input);
        Date first = input.get(0).startTime;
        Date last = input.get(input.size() - 1).completeTime;

        Calendar cal = Calendar.getInstance();
        cal.setTime(first);
        int stYear = cal.get(Calendar.YEAR);
        int stMonth = cal.get(Calendar.MONTH);
        int stDay = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(last);
        int enYear = cal.get(Calendar.YEAR);
        int enMonth = cal.get(Calendar.MONTH);
        int enDay = cal.get(Calendar.DAY_OF_MONTH);
        System.out.println("start: year: " + stYear + "; month: " + stMonth + "; day:" + stDay);
        System.out.println("end: year: " + enYear + "; month: " + enMonth + "; day:" + enDay);

        List<Date> splitPoints = new ArrayList<>();

        for (int y = stYear; y <= enYear; y++) {
            for (int mon = stMonth; mon <= enMonth; mon++) {
                for (int d = stDay; d <= enDay; d++) {
                    for (int pointer = 0;
                         pointer <= splitHours.length - 1 && pointer <= splitMinutes.length - 1; pointer++) {
                        cal.set(y, mon, d, splitHours[pointer], splitMinutes[pointer], 0);
                        splitPoints.add(cal.getTime());
                    }
                }
            }
        }
        //far-far future comes...
        splitPoints.add(new Date(Long.MAX_VALUE));

        System.out.println(splitPoints);

        List<List<SingleTuple>> output = new ArrayList<>();

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

        return output;
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
            tuples.add(SingleTuple.generateTuple(sign));
        }

        tuples = Util.sortTuples(tuples);
        return tuples;
    }
}