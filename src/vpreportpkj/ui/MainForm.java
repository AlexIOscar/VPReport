package vpreportpkj.ui;

import vpreportpkj.core.SingleTuple;
import vpreportpkj.core.Util;
import vpreportpkj.starter.ReportProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Properties;

import static java.net.URLDecoder.decode;

public class MainForm extends JFrame {
    private JPanel panel1;
    private JTextField chooseText;
    private JTextField outputDitText;
    private JTextField reportName;
    private JButton dealButton;
    private JButton cdButton;
    private JButton setOutDirButton;
    private final Properties prop = new Properties();
    private File propFile;

    public MainForm() throws HeadlessException, IOException {
        setContentPane(panel1);
        setVisible(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        ImageIcon ii = new ImageIcon(MainForm.class.getResource("link.png"));
        setIconImage(ii.getImage());

        setSize(780, 260);
        setLocation(100, 100);
        setTitle("VP reports processor");
        initChooseButton();
        initOutButton();
        initDealButton();
        initWindowListeners();
        initProperties();

        chooseText.setText(prop.getProperty("chooseText"));
        outputDitText.setText(prop.getProperty("outputDitText"));
        reportName.setText(prop.getProperty("reportName", "Report"));

        this.setSize(Integer.parseInt(prop.getProperty("formWidth", "780")),
                Integer.parseInt(prop.getProperty("formHeight", "260")));
    }

    private void initProperties() throws IOException {
        String addr = System.getProperty("user.home") + "\\VPRP\\";
        propFile = new File(addr + "app.properties");
        if (!propFile.exists()) {
            new File(addr).mkdir();
            propFile.createNewFile();
        }
        prop.load(new InputStreamReader(Files.newInputStream(propFile.toPath()), StandardCharsets.UTF_8));
    }

    private void initWindowListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                prop.setProperty("chooseText", chooseText.getText());
                prop.setProperty("outputDitText", outputDitText.getText());
                prop.setProperty("reportName", reportName.getText());
                prop.setProperty("formWidth", String.valueOf(getWidth()));
                prop.setProperty("formHeight", String.valueOf(getHeight()));
                try {
                    prop.store(new OutputStreamWriter(Files.newOutputStream(propFile.toPath()), StandardCharsets.UTF_8),
                            "storing props");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Ошибка взаимодействия с файлом конфигурации");
                    throw new RuntimeException(ex);
                }
                System.exit(0);
            }
        });
    }

    public void initChooseButton() {
        cdButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showDialog(null, "Choose");
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
            int dialogResult = fc.showDialog(null, "OK");
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                outputDitText.setText(selected.getAbsolutePath());
            }
        });
    }

    public void initDealButton() {
        dealButton.addActionListener(e -> {
            String path1 = chooseText.getText();
            String path2 = outputDitText.getText();
            List<SingleTuple> wholeList = null;
            if (path1.equals("")) {
                JOptionPane.showMessageDialog(null, "Input directory is empty");
                return;
            }

            if (path2.equals("")) {
                JOptionPane.showMessageDialog(null, "Output directory is empty");
                return;
            }

            try {
                wholeList = Util.getCommonList(chooseText.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "Wrong line format (generation tuple error)");
            } catch (Exception e2) {
                JOptionPane.showMessageDialog(null, e2.getMessage());
            }

            if (wholeList != null) {
                List<List<SingleTuple>> periods = Util.splitPeriods(wholeList, new int[]{8, 20});

                try {
                    ReportProcessor.pushToFile((outputDitText.getText() + "\\" + reportName.getText() + "_common.txt"),
                            wholeList);
                    ReportProcessor.pushToFileForList((outputDitText.getText() + "\\" + reportName.getText() + "_shifts.txt"),
                            periods);
                } catch (Exception nsfe) {
                    JOptionPane.showMessageDialog(null, nsfe.getMessage());
                }
            }
        });
    }
}
