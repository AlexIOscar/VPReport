package vpreportpkj.ui;

import vpreportpkj.core.*;
import vpreportpkj.core.labrepo.AdvancedRepo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

public class MainForm extends JFrame {
    String version = "v1.4";
    private JPanel reportPane;
    private JTextField chooseText;
    private JTextField outputDirText;
    private JTextField reportName;
    private JButton dealButton;
    private JButton cdButton;
    private JButton setOutDirButton;
    private JLabel srnLabel;
    private JTabbedPane jtp;
    private JPanel orderPane;
    private JTextField orderField;
    private JLabel ordLabel;
    private JButton ordProcButton;
    private JTextArea outOrdData;
    private JTextArea orderSrcField;
    private JButton orderSrcButton;
    private final Properties prop = new Properties();
    private File propFile;

    //LE со "старой" версией репозитория (второй репозиторий остается пустым)
    private LabourEngine labEng;

    //LE с "обновленной" версией репозитория (второй репозиторий остается пустым)
    private LabourEngine labEngComm;

    public MainForm() throws HeadlessException, IOException {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setContentPane(jtp);

        ImageIcon ii = new ImageIcon(MainForm.class.getResource("link.png"));
        setIconImage(ii.getImage());

        setSize(780, 260);
        setLocation(100, 100);
        createMenu();
        setTitle("VP reports processor " + version);
        initChooseButton();
        initOutDirButton();
        initDealButton();
        initOrdPrButton();
        initOrdSrcButton();
        initWindowListeners();
        initProperties();
        applySettings();

        //this is the last for prevent flickering
        setVisible(true);

        //обязателен вызов после applySettings, иначе состояние ReportProcessor будет дефолтным
        //вызываем после отображения основной формы, могут быть месседж-боксы
        if (ReportProcessor.useFastRepo) {
            setCursor(new Cursor(Cursor.WAIT_CURSOR));
            switchActivity(false);
            initFastRepo();
            initRepo();
            switchActivity(true);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void applySettings() {
        chooseText.setText(prop.getProperty("chooseText"));
        outputDirText.setText(prop.getProperty("outputDirText"));
        reportName.setText(prop.getProperty("reportName", "Report"));
        orderSrcField.setText(prop.getProperty("orderSrcDir"));

        this.setSize(Integer.parseInt(prop.getProperty("formWidth", "780")),
                Integer.parseInt(prop.getProperty("formHeight", "260")));

        if (prop.getProperty("lang", "1").equals("1")) {
            initRusUI();
        }

        //применяем настройки
        try {
            ReportProcessor.setSingleRBTime(Integer.parseInt(prop.getProperty("carRB", "50")));
            ReportProcessor.setGapLimit(Integer.parseInt(prop.getProperty("suspTG", "400")));
            ReportProcessor.setProcessingLimit(Integer.parseInt(prop.getProperty("suspPT", "600")));
            ReportProcessor.setShiftDuration(Integer.parseInt(prop.getProperty("shiftDur", "720")));
            ReportProcessor.setWhipLength(Integer.parseInt(prop.getProperty("whipLen", "12000")));
            ReportProcessor.setKim(Double.parseDouble(prop.getProperty("kim", "0.85")));
            ReportProcessor.setIsDecrSuspPT(Boolean.parseBoolean(prop.getProperty("decrSPTbox", "false")));
            ReportProcessor.setDecrSuspTTo(Integer.parseInt(prop.getProperty("decrSuspProcTo", "50")));
            ReportProcessor.setCRMMethodIndex(Integer.parseInt(prop.getProperty("CRBMethod", "0")));
            ReportProcessor.useFastRepo = Boolean.parseBoolean(prop.getProperty("useRepoRB", "false"));
            ReportProcessor.setUpdateRepo(Boolean.parseBoolean(prop.getProperty("updRepo", "true")));
            ReportProcessor.setFilterFactor(Integer.parseInt(prop.getProperty("ff", "4")));
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Wrong settings format, check settings values");
        }
    }

    private void createMenu() {
        JMenuBar mbar = new JMenuBar();
        this.setJMenuBar(mbar);
        JMenu fileMenu = new JMenu("File");
        JMenu aboutMenu = new JMenu("About");
        mbar.add(fileMenu);
        mbar.add(aboutMenu);
        JMenuItem aboutItem = new JMenuItem("About");
        JMenuItem settingsItem = new JMenuItem("Settings...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(settingsItem);
        fileMenu.add(exitItem);
        aboutMenu.add(aboutItem);

        settingsItem.addActionListener(e -> new SettingsForm("Settings", this));
        exitItem.addActionListener(e -> this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, getAbout(), "About program", JOptionPane.INFORMATION_MESSAGE));
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

    private String getAbout() {
        return "Version: " + version + "\nOEMZ IT Department, 2022\nDeveloped by Tolstokulakov A.V.\nLocal phone: 7096";
    }

    private void initWindowListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                prop.setProperty("chooseText", chooseText.getText());
                prop.setProperty("outputDirText", outputDirText.getText());
                prop.setProperty("reportName", reportName.getText());
                prop.setProperty("orderSrcDir", orderSrcField.getText());
                prop.setProperty("formWidth", String.valueOf(getWidth()));
                prop.setProperty("formHeight", String.valueOf(getHeight()));
                try {
                    prop.store(new OutputStreamWriter(Files.newOutputStream(propFile.toPath()), StandardCharsets.UTF_8),
                            "storing props");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Ошибка взаимодействия с файлом конфигурации");
                    throw new RuntimeException(ex);
                }
                if (ReportProcessor.useFastRepo) {
                    labEng.pushFastRepo();
                    try {
                        labEngComm.storeInstance();
                    } catch (IOException | InterruptedException ex) {
                        JOptionPane.showMessageDialog(null, "The repository file saving failed");
                    }
                }
                System.exit(0);
            }
        });
    }

    public void initChooseButton() {
        cdButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showDialog(this, "Choose");
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                chooseText.setText(selected.getAbsolutePath());
            }
        });
    }

    public void initOutDirButton() {
        setOutDirButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showDialog(this, "OK");
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                outputDirText.setText(selected.getAbsolutePath());
            }
        });
    }

    public void initFastRepo() {
        String dir = System.getProperty("user.home") + "\\VPRP\\";
        File repo = new File(dir + "pcFastRepo.dat");
        if (!repo.exists()) {
            if (new File(dir).exists()) {
                new File(dir).mkdir();
            }
            //получаем пустой движок, ассоциируем путь
            labEng = LabourEngine.getFastEngine(repo.getAbsolutePath());
            //и записываем его в файл
            labEng.pushFastRepo();
        }
        //не может прийти null, но может прийти пустой репозиторий
        labEng = LabourEngine.getFastEngine(repo.getAbsolutePath());
        ReportProcessor.le = labEng;
    }

    public void initRepo() {
        String dir = System.getProperty("user.home") + "\\VPRP\\";
        File repo = new File(dir + "pcRepo.dat");

        int ff = Integer.parseInt(prop.getProperty("ff", "4"));
        boolean upd = Boolean.parseBoolean(prop.getProperty("updRepo", "true"));

        if (!repo.exists()) {
            if (new File(dir).exists()) {
                new File(dir).mkdir();
            }
            JOptionPane.showMessageDialog(this, "Repo file doesn't exist, probably it's your first start\n" +
                    "New repository will be created");
            labEngComm = LabourEngine.getEmpty(null, repo.getAbsolutePath(), new AdvancedRepo(32, ff, upd));
            ReportProcessor.lec = labEngComm;
            return;
        }

        try {
            labEngComm = LabourEngine.getInstance(repo.getAbsolutePath());
            ((AdvancedRepo) labEngComm.getRepository()).setUpdate(upd);
            ((AdvancedRepo) labEngComm.getRepository()).setFilterFactor(ff);
            ((AdvancedRepo) labEngComm.getRepository()).setSb(new StringBuilder());
            ReportProcessor.lec = labEngComm;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Repository reading failure, the file may be corrupted");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "IOException: repository file is out of reach");
        }
    }

    public void initDealButton() {
        dealButton.addActionListener(e -> {

            String path1 = chooseText.getText();
            String path2 = outputDirText.getText();
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
                //Util.resolveTime(wholeList);
                List<List<SingleTuple>> periods;
                try {
                    int[] shiftHrs = Util.getShiftsSplits(prop.getProperty("shiftTimes", "8:00; 20:00")).get(0);
                    int[] shiftMin = Util.getShiftsSplits(prop.getProperty("shiftTimes", "8:00; 20:00")).get(1);
                    periods = Util.splitPeriods(wholeList, shiftHrs, shiftMin);
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Wrong format of shift times, check settings\n" + nfe.getMessage());
                    return;
                }

                try {
                    ReportProcessor.pushToFile((outputDirText.getText() + "\\" + reportName.getText() + "_common.txt"),
                            wholeList);
                    ReportProcessor.pushToFileForList((outputDirText.getText() + "\\" + reportName.getText() + "_shifts.txt"),
                            periods);
                    //Дальше лист кортежей следует в измененном (resolved) состоянии!
                    ReportProcessor.savePcsCSV((outputDirText.getText() + "\\" + reportName.getText() + "_pcs.csv"),
                            Util.resolveTime(wholeList));

                } catch (Exception nsfe) {
                    JOptionPane.showMessageDialog(null, "Unexpected exception");
                    nsfe.printStackTrace();
                }
            }
        });
    }

    //на второй форме
    private void initOrdPrButton() {
        ordProcButton.addActionListener(e -> {
            if (orderField.getText().equals("")) {
                JOptionPane.showMessageDialog(this, "Поле \"заказ\" не должно быть пустым");
                return;
            }
            outOrdData.setText("");
            StringBuilder sb = new StringBuilder();
            String[] paths = orderSrcField.getText().split("\n");

            for (String path : paths) {
                path = path.trim();
                if (path.equals("")) {
                    JOptionPane.showMessageDialog(null, "Directory area should contain at least one path, can't " +
                            "contain empty lines");
                }

                double coeff;
                try {
                    coeff = Double.parseDouble(prop.getProperty("timeCoeff", "1.0"));
                } catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Неверный формат коэффициента времени");
                    coeff = 1.0;
                }

                OrderSolver os = new OrderSolver(orderField.getText(), path,
                        Integer.parseInt(prop.getProperty("whipLen", "12000")),
                        Double.parseDouble(prop.getProperty("kim", "0.85")),
                        Integer.parseInt(prop.getProperty("carRB", "50")));
                sb.append("Processing time, min: ").append(String.format("%.3f",
                        os.getOrderTime(coeff, labEngComm) / 60)).append('\n');
                sb.append("Pcs count: ").append(os.countPcs()).append('\n');
            }
            outOrdData.setText(sb.toString());
        });
    }

    //на второй форме
    private void initOrdSrcButton() {
        orderSrcButton.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int dialogResult = fc.showDialog(this, "Choose");
            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                File selected = fc.getSelectedFile();
                orderSrcField.setText(orderSrcField.getText()
                        + (orderSrcField.getText().equals("") ? "" : '\n')
                        + selected.getAbsolutePath());
            }
        });
    }

    protected void initRusUI() {
        getSrnLabel().setText("Сохранить с именем");
        getCdButton().setText("Директория-источник");
        getSetOutDirButton().setText("Сохранить в директорию");
        getDealButton().setText("Создать отчет");
        getJMenuBar().getMenu(0).getItem(0).setText("Настройки...");
        getJMenuBar().getMenu(0).getItem(1).setText("Выход");
        jtp.setTitleAt(0, "Отчет");
        jtp.setTitleAt(1, "Заказ");
        orderSrcButton.setText("Источник");
        ordLabel.setText("Заказ");
        ordProcButton.setText("Вычислить");
    }

    private void switchActivity(boolean isEnabled) {
        setOutDirButton.setEnabled(isEnabled);
        cdButton.setEnabled(isEnabled);
        dealButton.setEnabled(isEnabled);
        chooseText.setEnabled(isEnabled);
        outputDirText.setEnabled(isEnabled);
        reportName.setEnabled(isEnabled);
        srnLabel.setEnabled(isEnabled);
    }

    public Properties getProp() {
        return prop;
    }

    public JLabel getSrnLabel() {
        return srnLabel;
    }

    public JButton getDealButton() {
        return dealButton;
    }

    public JButton getCdButton() {
        return cdButton;
    }

    public JButton getSetOutDirButton() {
        return setOutDirButton;
    }
}