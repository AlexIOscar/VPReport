package vpreportpkj.core;
import java.util.List;
import java.util.stream.Collectors;

public class OrderSolver {
    private final int whipLength;
    private final double MUR;
    private final int rollbackTime;
    private final String order;
    private final String path;

    public OrderSolver(String order, String path, int whipLength, double MUR, int rollbackTime) {
        this.whipLength = whipLength;
        this.MUR = MUR;
        this.rollbackTime = rollbackTime;
        this.order = order;
        this.path = path;
    }

    public double getOrderTime(double multiplier, LabourEngine le) {
        List<SingleTuple> tuples = Util.getCommonList(path);
        Util.resolveTime(tuples);

        List<SingleTuple> filtered = tuples.stream().filter(t -> t.order.equals(order)).collect(Collectors.toList());
        filterViaRepo(filtered, le);

        int sumDuration = filtered.stream().mapToInt(t -> t.duration).sum();
        double sumLength = filtered.stream().mapToDouble(t -> t.length).sum();
        return ((sumDuration * multiplier) + (sumLength / MUR / whipLength) * rollbackTime);
    }

    public int countPcs() {
        return (int) Util.getCommonList(path).stream().filter(t -> t.order.equals(order)).count();
    }

    private void filterViaRepo(List<SingleTuple> tuples, LabourEngine le) {
        for (SingleTuple st : tuples) {
            int expTime = le.getRepository().chkTime(st);
            if (expTime == st.getDuration()) {
                continue;
            }
            st.duration = expTime;
        }
    }
}