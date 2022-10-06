package vpreportpkj.ui;

import vpreportpkj.core.SingleTuple;
import vpreportpkj.core.Util;
import vpreportpkj.starter.ReportProcessor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

import static vpreportpkj.starter.ReportProcessor.getReport;

public class MainForm extends JFrame {
    private JPanel panel1;
    private JTextField chooseText;
    private JTextField reportName;
    private JButton dealButton;
    private JButton cdButton;
    private JTextField outputDitText;
    private JButton setOutDirButton;

    public MainForm() throws HeadlessException {
        setContentPane(panel1);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 300);
        setLocation(100, 100);
        initChooseButton();
        initOutButton();
        initDealButton();


    }

    public void initChooseButton() {
        cdButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showOpenDialog(null);
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                chooseText.setText(selected.getAbsolutePath());
            }
        });
    }

    public void initOutButton() {
        setOutDirButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showSaveDialog(null);
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                outputDitText.setText(selected.getAbsolutePath());
            }
        });
    }

    public void initDealButton() {
        dealButton.addActionListener(e -> {
            String path = chooseText.getText();
            List<SingleTuple> wholeList = null;
            if (!path.equals("")) {
                try {
                    wholeList = Util.getCommonList(chooseText.getText());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Wrong line format (generation tuple error)");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Input directory is empty");
                return;
            }
            if (wholeList != null) {
                List<List<SingleTuple>> periods = Util.splitPeriods(wholeList, new int[]{8, 20});

                ReportProcessor.pushToFile((outputDitText.getText() + "\\" + reportName.getText() + "_common.txt"),
                        wholeList);
                ReportProcessor.pushToFileForList((outputDitText.getText() + "\\" + reportName.getText() + "_shifts.txt"),
                        periods);
            }
        });
    }
}
