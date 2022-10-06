package vpreportpkj.starter;
import vpreportpkj.ui.MainForm;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        MainForm mf = new MainForm();

        //String path2 = "C:\\IntellijProj\\VPReport\\src\\20_09_2022.txt";
        String path = "C:\\VPReportsTest\\29_09_2022_morn.txt";

        /*
        List<SingleTuple> tuples = Util.getTuplesList(path);
        List<List<SingleTuple>> periods = Util.splitPeriods(tuples, new int[]{8, 20});

        List<String> rep = getReportForList(periods, true, true);
        rep.forEach(System.out::println);
         */

        /*
        //склеиваем все отчеты в папке в единый пул
        List<SingleTuple> commonList = Util.getCommonList("C:\\VPReportsTest");
        //общий отчет
        List<String> rep2 = getReport(commonList, false, false);
        List<List<SingleTuple>> periods = Util.splitPeriods(commonList, new int[]{8, 20});
        //посменный отчет
        List<String> rep3 = getReportForList(periods, false, false);
        rep2.forEach(System.out::println);
        System.out.println("\n\n____________Посменно:_____________");
        rep3.forEach(System.out::println);
         */

    }
}
