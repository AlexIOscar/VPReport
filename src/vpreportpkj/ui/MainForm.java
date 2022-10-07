package vpreportpkj.ui;

import vpreportpkj.core.SingleTuple;
import vpreportpkj.core.Util;
import vpreportpkj.starter.ReportProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;

import static vpreportpkj.starter.ReportProcessor.getReport;

public class MainForm extends JFrame {
    private JPanel panel1;
    private JTextField chooseText;
    private JTextField outputDitText;
    private JTextField reportName;
    private JButton dealButton;
    private JButton cdButton;
    private JButton setOutDirButton;
    private final Properties prop = new Properties();
    private String propPath;

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
        initProperties();
        initWindowListeners();

        chooseText.setText(prop.getProperty("chooseText"));
        outputDitText.setText(prop.getProperty("outputDitText"));
        reportName.setText(prop.getProperty("reportName"));
        this.setSize(Integer.parseInt(prop.getProperty("formWidth")), Integer.parseInt(prop.getProperty("formHeight")));
    }

    private void initProperties() throws IOException {
        try {
            URI uri = Thread.currentThread().getContextClassLoader().getResource("").toURI();
            propPath = Paths.get(uri) + "\\";
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        propPath = URLDecoder.decode(propPath, StandardCharsets.UTF_8);
        propPath = propPath + "app.properties";

        prop.load(new InputStreamReader(Files.newInputStream(new File(propPath).toPath()), StandardCharsets.UTF_8));
    }

    private void initWindowListeners(){
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                prop.setProperty("chooseText", chooseText.getText());
                prop.setProperty("outputDitText", outputDitText.getText());
                prop.setProperty("reportName", reportName.getText());
                prop.setProperty("formWidth", String.valueOf(getWidth()));
                prop.setProperty("formHeight", String.valueOf(getHeight()));
                try {
                    prop.store(new OutputStreamWriter(Files.newOutputStream(new File(propPath).toPath()), StandardCharsets.UTF_8), "storing props");
                } catch (IOException ex) {
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
