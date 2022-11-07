package vpreportpkj.ui;

import vpreportpkj.core.ReportProcessor;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;

public class SettingsForm extends JFrame {

    private JPanel panel;
    private JButton saveSettButton;
    private JTextField carRBfield;
    private JTextField suspTGfield;
    private JTextField suspPTfild;
    private JTextField shiftDurField;
    private JTextField whipLenField;
    private JTextField kimField;
    private JCheckBox decrCBox;
    private JTextField decrToField;
    private JComboBox<String> CRBMcomboBox;
    private JTextField shiftsField;
    private JLabel RBCTimeLabel;
    private JLabel suspTGLabel;
    private JLabel suspPTLabel;
    private JLabel shDurLabel;
    private JLabel shPtsLabel;
    private JLabel whipLenLabel;
    private JLabel kimLabel;
    private JLabel CRBMethLabel;
    private JComboBox<String> langComBox;
    private JLabel langLabel;
    private JRadioButton bruteDecrRB;
    private JRadioButton smartDecrRB;
    private JCheckBox updRepoCB;
    private JTextField ffField;
    private JLabel ffLabel;
    private JTextField timeCoeffField;
    private JLabel timeCoeffLabel;

    public SettingsForm(String title, MainForm parent) throws HeadlessException {
        super(title);
        ImageIcon ii = new ImageIcon(MainForm.class.getResource("link.png"));
        setIconImage(ii.getImage());
        setSize(500, 350);
        setLocationRelativeTo(null);
        setContentPane(panel);

        initSettings(parent.getProp());
        initSaveButton(parent.getProp(), parent);
        initActiveElements();

        if (parent.getProp().getProperty("lang", "1").equals("1")) {
            initRusUI(parent);
        }

        pack();
        this.setVisible(true);
    }

    public void initSettings(Properties props) {
        //читаем настройки в поля формы
        carRBfield.setText(props.getProperty("carRB", "50"));
        suspTGfield.setText(props.getProperty("suspTG", "400"));
        suspPTfild.setText(props.getProperty("suspPT", "600"));
        shiftDurField.setText(props.getProperty("shiftDur", "720"));
        whipLenField.setText(props.getProperty("whipLen", "12000"));
        kimField.setText(props.getProperty("kim", "0.85"));
        decrToField.setText(props.getProperty("decrSuspProcTo", "50"));
        shiftsField.setText(props.getProperty("shiftTimes", "8:00;  20:00"));
        bruteDecrRB.setSelected(Boolean.parseBoolean(props.getProperty("decrSuspRB", "true")));
        smartDecrRB.setSelected(Boolean.parseBoolean(props.getProperty("useRepoRB", "false")));
        CRBMcomboBox.setSelectedIndex(Integer.parseInt(props.getProperty("CRBMethod", "0")));
        langComBox.setSelectedIndex(Integer.parseInt(props.getProperty("lang", "1")));
        decrCBox.setSelected(Boolean.parseBoolean(props.getProperty("decrSPTbox", "false")));
        updRepoCB.setSelected(Boolean.parseBoolean(props.getProperty("updRepo", "true")));
        ffField.setText(props.getProperty("ff", "4"));
        timeCoeffField.setText(props.getProperty("timeCoeff", "1.0"));

        //init changeable elements states
        bruteDecrRB.setEnabled(decrCBox.isSelected());
        smartDecrRB.setEnabled(decrCBox.isSelected());
        decrToField.setEnabled(bruteDecrRB.isSelected() && bruteDecrRB.isEnabled());
        ffLabel.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
        updRepoCB.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
        ffField.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
    }

    public void initSaveButton(Properties props, MainForm mf) {
        saveSettButton.addActionListener(e -> {
            //форсим изменения в обработчик отчетов
            try {
                ReportProcessor.setSingleRBTime(Integer.parseInt(carRBfield.getText()));
                ReportProcessor.setGapLimit(Integer.parseInt(suspTGfield.getText()));
                ReportProcessor.setProcessingLimit(Integer.parseInt(suspPTfild.getText()));
                ReportProcessor.setShiftDuration(Integer.parseInt(shiftDurField.getText()));
                ReportProcessor.setWhipLength(Integer.parseInt(whipLenField.getText()));
                ReportProcessor.setKim(Double.parseDouble(kimField.getText()));
                ReportProcessor.setIsDecrSuspPT(decrCBox.isSelected());
                ReportProcessor.setDecrSuspTTo(Integer.parseInt(decrToField.getText()));
                ReportProcessor.setCRMMethodIndex(CRBMcomboBox.getSelectedIndex());
                ReportProcessor.useFastRepo = smartDecrRB.isSelected();
                ReportProcessor.setUpdateRepo(updRepoCB.isSelected());
                ReportProcessor.setFilterFactor(Double.parseDouble(ffField.getText()));
                if (smartDecrRB.isSelected() && smartDecrRB.isEnabled()) {
                    mf.initFastRepo();
                }

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Wrong settings format, check settings values");
                return;
            }
            //и сохраняем в настройках
            props.setProperty("carRB", carRBfield.getText());
            props.setProperty("suspTG", suspTGfield.getText());
            props.setProperty("suspPT", suspPTfild.getText());
            props.setProperty("shiftDur", shiftDurField.getText());
            props.setProperty("whipLen", whipLenField.getText());
            props.setProperty("kim", kimField.getText());
            props.setProperty("decrSuspProcTo", decrToField.getText());
            props.setProperty("shiftTimes", shiftsField.getText());
            props.setProperty("decrSuspRB", bruteDecrRB.isSelected() ? "true" : "false");
            props.setProperty("useRepoRB", smartDecrRB.isSelected() ? "true" : "false");
            props.setProperty("CRBMethod", String.valueOf(CRBMcomboBox.getSelectedIndex()));
            props.setProperty("lang", String.valueOf(langComBox.getSelectedIndex()));
            props.setProperty("decrSPTbox", decrCBox.isSelected() ? "true" : "false");
            props.setProperty("ff", ffField.getText());
            props.setProperty("updRepo", updRepoCB.isSelected() ? "true" : "false");
            props.setProperty("timeCoeff", timeCoeffField.getText());

            dispose();
        });
    }

    private void initActiveElements() {
        decrCBox.addChangeListener(e -> {
            bruteDecrRB.setEnabled(decrCBox.isSelected());
            smartDecrRB.setEnabled(decrCBox.isSelected());
            decrToField.setEnabled(bruteDecrRB.isSelected() && bruteDecrRB.isEnabled());
        });

        bruteDecrRB.addChangeListener(e -> decrToField.setEnabled(bruteDecrRB.isSelected()));

        smartDecrRB.addChangeListener(e -> {
            ffLabel.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
            updRepoCB.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
            ffField.setEnabled(smartDecrRB.isSelected() && smartDecrRB.isEnabled());
        });
    }

    /**
     * Подход к локализации довольно кривой: все формы сначала строятся с английским, но если в свойствах установлен
     * русский, то все лейблы заменяются на русские еще до отображения форм
     *
     * @param parent форма-родитель (заглавная)
     */
    private void initRusUI(MainForm parent) {
        parent.getSrnLabel().setText("Сохранить с именем");
        parent.getCdButton().setText("Директория-источник");
        parent.getSetOutDirButton().setText("Сохранить в директорию");
        parent.getDealButton().setText("Создать отчет");
        parent.getJMenuBar().getMenu(0).getItem(0).setText("Настройки...");
        parent.getJMenuBar().getMenu(0).getItem(1).setText("Выход");
        saveSettButton.setText("Сохранить");
        RBCTimeLabel.setText("Время отката каретки, сек");
        suspPTLabel.setText("Подозрительное время обработки, больше чем, сек");
        suspTGLabel.setText("Подозрительный перерыв, больше чем, сек");
        bruteDecrRB.setText("Уменьшать подозр. время обработки до, сек:");
        shDurLabel.setText("Длительность смены, минут");
        shPtsLabel.setText("Точки разбиения на смены, список через \";\"");
        whipLenLabel.setText("Длина единицы сырья, мм");
        kimLabel.setText("Коэффициент использования металла");
        CRBMethLabel.setText("Методика детектирования откатов каретки");
        CRBMcomboBox.removeAllItems();
        CRBMcomboBox.addItem("по количеству рубов");
        CRBMcomboBox.addItem("по паузам в обработке");
        CRBMcomboBox.addItem("по общей обработанной длине");
        langLabel.setText("Выбрать язык интерфейса (требуется перезапуск)");
        smartDecrRB.setText("Использовать внутреннее хранилище данных о времени");
        decrCBox.setText("Адаптировать время выполнения операций");
        ffLabel.setText("Фильтр-фактор");
        updRepoCB.setText("Обновлять хранилище входящими данными");
        timeCoeffLabel.setText("Коэффициент (времени)");
    }
}
