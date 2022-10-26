package vpreportpkj.starter;

import vpreportpkj.core.OrderSolver;

public class DebugMain {
    public static void main(String[] args) {
        String path164 = "C:\\Users\\Tolstokulakov_AV\\Desktop\\164_report";
        String path94 = "C:\\Users\\Tolstokulakov_AV\\Desktop\\94_report";
        String order = "22901";
        OrderSolver os164 = new OrderSolver(order, path164);
        OrderSolver os94 = new OrderSolver(order, path94);
        double time94 = os94.getOrderTime(1.2)/3600;
        double time164 = os164.getOrderTime(1.2)/3600;
        int count94 = os94.countPcs();
        int count164 = os164.countPcs();
        System.out.println(time94 + ", pcs count: " + count94);
        System.out.println(time164 + ", pcs count: " + count164);
        System.out.println("total pcs: "+ (count94 + count164));
        System.out.println("total time: " + (time94 + time164));

    }
}