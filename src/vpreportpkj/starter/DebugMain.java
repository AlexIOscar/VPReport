package vpreportpkj.starter;

import vpreportpkj.core.LabourEngine;
import vpreportpkj.core.SingleTuple;
import vpreportpkj.core.Util;
import vpreportpkj.core.labrepo.AdvancedRepo;

import java.io.IOException;
import java.util.List;

public class DebugMain {
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        LabourEngine instance = LabourEngine.getInstance("C:\\Users\\user\\VPRP\\pcRepo.dat");
        List<SingleTuple> commonList = Util.getCommonList("C:\\Users\\user\\Desktop\\тестовый каталог IN");

        AdvancedRepo repo = (AdvancedRepo) instance.getRepository();
        repo.setUpdate(false);
        repo.setFilterFactor(2);
        for (SingleTuple st: commonList
             ) {
            System.out.println(repo.chkTime(st));
        }
    }
}