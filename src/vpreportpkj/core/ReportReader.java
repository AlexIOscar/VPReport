package vpreportpkj.core;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReportReader {
    /**
     * read the file by input path, divide to Strings and return as List
     * @param filePath the file read to
     * @return List of String-lines from raw report
     */
    public static List<String> getStrArr(String filePath) {
        List<String> lineList = new ArrayList<>();

        File f = new File(filePath);
        if (!f.exists()) {
            System.out.println("file does not exist");
            return null;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "CP1251"))) {
            String line = br.readLine();
            while (line != null) {
                lineList.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return lineList;
    }
}
