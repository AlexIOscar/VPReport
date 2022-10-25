package vpreportpkj.starter;

import vpreportpkj.core.OrderSolver;

public class DebugMain {
    public static void main(String[] args) {
        String path164 = "C:\\Users\\Tolstokulakov_AV\\Desktop\\164_report";
        String path94 = "C:\\Users\\Tolstokulakov_AV\\Desktop\\94_report";
        OrderSolver os164 = new OrderSolver("22616", path164);
        OrderSolver os94 = new OrderSolver("22616", path94);
        System.out.println(os94.getOrderTime(1.2)/3600);
        System.out.println(os164.getOrderTime(1.2)/3600);
    }
}