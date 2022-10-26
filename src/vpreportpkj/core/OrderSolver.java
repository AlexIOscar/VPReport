package vpreportpkj.core;

import vpreportpkj.core.labrepo.AdvancedRepo;
import vpreportpkj.core.labrepo.LabourRepository;

import java.io.IOException;
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
        Util.resolveTime(tuples);

        List<SingleTuple> filtered = tuples.stream().filter(t -> t.order.equals(order)).collect(Collectors.toList());
        filterViaRepo(filtered);

        int sumDuration = filtered.stream().mapToInt(t -> t.duration).sum();
        double sumLength = filtered.stream().mapToDouble(t -> t.length).sum();
        return ((sumDuration * multiplier) + (sumLength / MUR / whipLength) * rollbackTime);
    }

    public int countPcs() {
        return (int) Util.getCommonList(path).stream().filter(t -> t.order.equals(order)).count();
    }

    private void filterViaRepo(List<SingleTuple> tuples) {
        LabourEngine le;
        try {
            le = LabourEngine.getInstance("C:\\Users\\Tolstokulakov_AV\\VPRP\\pcRepo.dat");
            ((AdvancedRepo) le.getRepository()).setSb(new StringBuilder());
            ((AdvancedRepo) le.getRepository()).setFilterFactor(4);
            ((AdvancedRepo) le.getRepository()).setUpdate(false);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            System.out.println("The repo has incompatible version or may be corrupted");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (SingleTuple st : tuples) {
            int expTime = le.getRepository().chkTime(st);
            if (expTime == st.getDuration()) {
                continue;
            }
            st.duration = expTime;
        }
    }
}