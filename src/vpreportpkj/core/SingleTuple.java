package vpreportpkj.core;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс представляет собой кортеж данных, связанных с обработкой одиночной детали на VP-оборудовании
 */
public class SingleTuple {
    Date completeTime;
    Date startTime;
    String order;
    String mark;
    String position;
    String roll;
    double length;
    int duration;
    int holeCount;
    double mass;
    int cuts;

    /**
     * Метод генерации кортежа из входной специальным образом форматированной строки
     * @param line входная строка
     * @return сгенерированный кортеж
     * @throws NumberFormatException в случае, если генерация кортежа была прервана по причине некорректного
     * форматирования строки или некорректного содержимого
     */
    public static SingleTuple generateTuple(String line) throws NumberFormatException{
        SingleTuple st = new SingleTuple();
        List<String> splitData = Arrays.stream(line.split(";"))
                .map(String::trim)
                .collect(Collectors.toList());

        if (splitData.size() < 18) {
            System.out.println("Wrong line format (generation tuple error)");
            throw new NumberFormatException();
        }

        List<String> date = Arrays.stream(splitData.get(0).split("/")).collect(Collectors.toList());
        List<String> time = Arrays.stream(splitData.get(6).split(":")).collect(Collectors.toList());

        st.completeTime = getDate(date, time);
        st.order = splitData.get(1);
        st.mark = splitData.get(3);
        int duration;
        try {
            st.holeCount = Integer.parseInt(splitData.get(15));
            st.mass = Double.parseDouble(splitData.get(9));
            st.length = Double.parseDouble(splitData.get(5));
            st.cuts = Integer.parseInt(splitData.get(17));
            duration = Integer.parseInt(splitData.get(7));
        } catch (NumberFormatException nfe) {
            System.out.println("Wrong line format: number format exception");
            throw nfe;
        }
        st.position = splitData.get(2);
        st.roll = splitData.get(4);
        st.startTime = new Date(st.completeTime.getTime() - duration * 1000L);
        st.duration = duration;
        return st;
    }

    /**
     * Генерирует временную метку на основании поданных на вход троек даты и времени (тройки передаются как List)
     * @param date тройка даты
     * @param time тройка времени
     * @return Построенная временная метка
     */
    private static Date getDate(List<String> date, List<String> time) {
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(date.get(2)),
                Integer.parseInt(date.get(1)) - 1,
                Integer.parseInt(date.get(0)),
                Integer.parseInt(time.get(0)),
                Integer.parseInt(time.get(1)),
                Integer.parseInt(time.get(2)));
        return cal.getTime();
    }

    @Override
    public String toString() {
        return "SingleTuple{" +
                "completeTime=" + completeTime +
                ", startTime=" + startTime +
                ", order='" + order + '\'' +
                ", mark='" + mark + '\'' +
                ", position='" + position + '\'' +
                ", roll='" + roll + '\'' +
                ", length=" + length +
                ", duration=" + duration +
                ", mass=" + mass +
                '}';
    }

    public Date getCompleteTime() {
        return completeTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String getOrder() {
        return order;
    }

    public String getMark() {
        return mark;
    }

    public String getPosition() {
        return position;
    }

    public String getRoll() {
        return roll;
    }

    public double getLength() {
        return length;
    }

    public int getDuration() {
        return duration;
    }

    public double getMass() {
        return mass;
    }

    public int getHoleCount() {
        return holeCount;
    }

    public int getCuts() {
        return cuts;
    }
}
