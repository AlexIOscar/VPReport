package vpreportpkj.core;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

    public static SingleTuple generateTuple(String line){
        SingleTuple st = new SingleTuple();
        List<String> splitData = Arrays.stream(line.split(";"))
                .map(String::trim)
                .collect(Collectors.toList());

        List<String> date = Arrays.stream(splitData.get(0).split("/")).collect(Collectors.toList());
        List<String> time = Arrays.stream(splitData.get(6).split(":")).collect(Collectors.toList());

        st.completeTime = getDate(date, time);
        st.order = splitData.get(1);
        st.mark = splitData.get(3);
        st.holeCount = Integer.parseInt(splitData.get(15));
        st.position = splitData.get(2);
        st.mass = Double.parseDouble(splitData.get(9));
        st.length = Double.parseDouble(splitData.get(5));
        int duration = Integer.parseInt(splitData.get(7));
        st.roll = splitData.get(4);
        st.startTime = new Date(st.completeTime.getTime() - duration * 1000L);
        st.duration = duration;
        return st;
    }

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
}
