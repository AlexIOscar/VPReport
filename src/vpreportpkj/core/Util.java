package vpreportpkj.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    private static final String dateFormatter = "dd/MM/yyyy—HH.mm.ss";

    /**
     * Метод, разбивающий входной список кортежей на части. Каждая часть содержит только
     * кортежи, лежащие внутри интервалов, на которые разбивается весь охватываемый кортежами период суточными
     * метками, передаваемыми в виде пары массивов splitHours и splitMinutes. Суточная метка
     * образуется путем объединения значений из этих двух массивов с равными индексами.
     *
     * @param input        входной список кортежей
     * @param splitHours   массив часовых суточных меток
     * @param splitMinutes массив минутных суточных меток
     * @return Разбитый на части список кортежей в виде списка списков.
     */
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

    /**
     * Генерирует список временных точек, по которым необходимо разбить временной интервал между first и last,
     * используя суточные метки времени, содержащиеся в паре массивов splitHours и splitMinutes. Суточная метка
     * образуется путем объединения значений из этих двух массивов с равными индексами.
     *
     * @param splitHours   массив часовых суточных меток
     * @param splitMinutes массив минутных суточных меток
     * @param first        дата начала разбиваемого интервала
     * @param last         дата окончания разбиваемого интервала
     * @return Список временных меток, разбивающих интервал
     */
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

    /**
     * Перегруженная версия, разбивающая список кортежей на части только по часовым суточным меткам
     *
     * @param input      входящий список кортежей
     * @param splitHours список часовых суточных меток
     * @return Разбитый на части список кортежей в виде списка списков.
     */
    public static List<List<SingleTuple>> splitPeriods(List<SingleTuple> input, int[] splitHours) {
        int[] spMin = new int[splitHours.length];
        return splitPeriods(input, splitHours, spMin);
    }

    /**
     * Метод сортирует кортежи по дате начала выполнения операции (содержащейся в кортеже).
     * Важно! Нарушать порядок сортировки (менять компаратор), заданный в этом методе, нельзя, без переработки метода
     * splitPeriods - это приведет к появлению "дребезга" на границах "смен".
     *
     * @param inputList входящий неотсортированный кортеж
     * @return исходящий отсортированный кортеж
     */
    public static List<SingleTuple> sortTuples(List<SingleTuple> inputList) {
        return inputList.stream()
                .sorted((t1, t2) -> (int) (t1.getStartTime().getTime() / 1000 - t2.getStartTime().getTime() / 1000))
                .collect(Collectors.toList());
    }

    /**
     * Преобразует файл сырого отчета по указанному пути в список неотсортированных кортежей
     *
     * @param path Путь размещения сырого отчета
     * @return Список кортежей
     */
    public static List<SingleTuple> getTuplesList(String path) {
        List<String> strArr = ReportReader.getStrArr(path);
        assert strArr != null;

        List<SingleTuple> tuples = new ArrayList<>();
        for (String sign : strArr) {
            SingleTuple tuple = SingleTuple.generateTuple(sign);
            tuples.add(tuple);
        }
        return tuples;
    }

    /**
     * Получение единого сортированного списка кортежей по всем файлам, размещенным в директории, без обхода в глубину
     *
     * @param path Директория, по которой собираются данные
     * @return Список отсортированных кортежей
     */
    public static List<SingleTuple> getCommonList(String path) {

        List<Path> paths = null;
        //File list creation
        try (Stream<Path> pstr = Files.list(Paths.get(path))) {
            paths = pstr.filter(Files::isRegularFile)
                    .filter(f -> f.toString().contains(".txt"))
                    .collect(Collectors.toList());
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        assert paths != null;
        Collections.reverse(paths);
        List<SingleTuple> finalOutList = new ArrayList<>();
        paths.forEach(p -> finalOutList.addAll(getTuplesList(p.toString())));
        return sortTuples(finalOutList);
    }

    /**
     * Генерирует пару массивов равной длины, каждый из которых содержит целые числа, соответствующие часам (нулевой
     * массив) и минутам (первый массив) суточных меток времени, для разбиения списков кортежей на смены
     *
     * @param formattedInput отформатированный строковый вход
     * @return пара массивов в виде двухместного листа
     * @throws NumberFormatException в случае неверного входного формата строки
     */
    public static List<int[]> getShiftsSplits(String formattedInput) throws NumberFormatException {
        List<int[]> out = new ArrayList<>(2);
        formattedInput = formattedInput.replaceAll(" +", "");
        String[] shiftTimes = formattedInput.split(";");
        //hrs
        out.add(new int[shiftTimes.length]);
        //min
        out.add(new int[shiftTimes.length]);
        for (int i = 0; i < shiftTimes.length; i++) {
            String[] hrs_min = shiftTimes[i].split(":");
            if (hrs_min.length != 2) {
                throw new NumberFormatException("It's necessary to set at least one pair in HH:MM format");
            }
            out.get(0)[i] = Integer.parseInt(hrs_min[0]);
            out.get(1)[i] = Integer.parseInt(hrs_min[1]);
        }
        return out;
    }

    //Изменяет кортежи во входящем листе!
    public static List<SingleTuple> resolveTime(List<SingleTuple> inputList) {
        List<List<SingleTuple>> bundles = splitToBundles(inputList);
        return bundles.stream().peek(Util::resolveBundle).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private static List<List<SingleTuple>> splitToBundles(List<SingleTuple> inputList) {
        List<List<SingleTuple>> bundles = new ArrayList<>();

        if (inputList.size() == 0) {
            bundles.add(new ArrayList<>());
            return bundles;
        }

        inputList = sortTuples(inputList);

        for (int i = 0; i < inputList.size(); i++) {
            List<SingleTuple> buffer = new ArrayList<>();
            buffer.add(inputList.get(i));
            Date latestInGroup = inputList.get(i).completeTime;
            for (int j = i + 1; j < inputList.size(); j++) {
                if (inputList.get(j).getStartTime().after(latestInGroup)) {
                    i = j - 1;
                    break;
                }
                buffer.add(inputList.get(j));
                if (inputList.get(j).completeTime.after(latestInGroup)) {
                    latestInGroup = inputList.get(j).completeTime;
                }
            }
            bundles.add(buffer);
        }
        return bundles;
    }

    /**
     * Метод предназначен для перераспределения времени между деталями внутри списка таким образом, чтобы таймлайн
     * был занят всеми деталями, вошедшими в список, без перекрытия и плотно (без пустот, и с "касанием" границ)
     *
     * @param bundle Специально подготовленный список кортежей, отвечающий требованиям: 1) он уже отсортирован по
     *               стартовому времени 2) он не содержит внутренних "пустот" в общем таймлайне. Внутри метода это
     *               никак не проверяется, и должно быть обеспечено клиентским кодом
     */
    private static void resolveBundle(List<SingleTuple> bundle) {

        if (bundle.size() < 2) {
            return;
        }

        int totalDur = bundle.stream().mapToInt(t -> t.duration).sum();
        if (totalDur == 0) {
            return;
        }

        Date start = bundle.get(0).startTime;
        Date complete = bundle.get(0).completeTime;
        for (int i = 1; i < bundle.size(); i++) {
            if (bundle.get(i).completeTime.after(complete)) {
                complete = bundle.get(i).completeTime;
            }
        }
        long timeFund = (complete.getTime() - start.getTime()) / 1000;
        double coeff = ((double) timeFund) / totalDur;

        //двигаем время окончания нулевого кортежа в соответствии с пересчитанной длительностью
        long firstDur = Math.round(bundle.get(0).duration * coeff);
        bundle.get(0).completeTime = new Date(bundle.get(0).startTime.getTime() + firstDur * 1000);
        bundle.get(0).duration = bundle.get(0).getDurViaCount();

        for (int i = 1; i < bundle.size(); i++) {
            SingleTuple st = bundle.get(i);
            long resolvedDur = Math.round(st.duration * coeff);
            st.startTime = bundle.get(i - 1).getCompleteTime();
            //st.duration = (int) resolvedDur;
            if (i == bundle.size() - 1) {
                st.duration = st.getDurViaCount();
                break;
            }
            st.completeTime = new Date(st.startTime.getTime() + resolvedDur * 1000);
            st.duration = st.getDurViaCount();
        }

        int totalDurAfter = bundle.stream().mapToInt(t -> t.duration).sum();
        if (totalDurAfter != timeFund) {
            System.out.println("Duration violation: " + totalDurAfter + " " + timeFund);
        }

    }

    public static String getFormattedDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatter);
        return simpleDateFormat.format(date);
    }
}