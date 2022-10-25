package vpreportpkj.core;

import java.util.List;
import java.util.stream.Collectors;

public class OrderSolver {
    static int whipLength = 12000;
    static double MUR = 0.85;
    static int rollbackTime = 50;
    private final String order;
    private final String path;

    public OrderSolver(String order, String path) {
        this.order = order;
        this.path = path;
    }

    public double getOrderTime(double multiplier) {
        List<SingleTuple> tuples = Util.getCommonList(path);
        tuples = Util.sortTuples(tuples);
        Util.resolveTime(tuples);
        List<SingleTuple> filtered = tuples.stream().filter(t -> t.order.equals(order)).collect(Collectors.toList());
        int sumDuration = filtered.stream().mapToInt(t -> t.duration).sum();
        double sumLength = filtered.stream().mapToDouble(t -> t.length).sum();
        return ((sumDuration * multiplier) + (sumLength / MUR / whipLength) * rollbackTime);
    }
}