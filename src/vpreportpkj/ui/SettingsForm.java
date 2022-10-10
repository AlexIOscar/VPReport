package vpreportpkj.ui;

import vpreportpkj.starter.ReportProcessor;

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
    private JLabel cRBDetectingField;

    public SettingsForm(String title, MainForm parent) throws HeadlessException {
        super(title);
        ImageIcon ii = new ImageIcon(MainForm.class.getResource("link.png"));
        setIconImage(ii.getImage());
        setSize(500, 350);
        setLocationRelativeTo(null);
        setContentPane(panel);

        initSettings(parent.getProp());
        initSaveButton(parent.getProp());
        initChkBoxes();

        this.setVisible(true);
    }

    public void initSettings(Properties props) {
        carRBfield.setText(props.getProperty("carRB", "50"));
        suspTGfield.setText(props.getProperty("suspTG", "400"));
        suspPTfild.setText(props.getProperty("suspPT", "600"));
        shiftDurField.setText(props.getProperty("shiftDur", "720"));
        whipLenField.setText(props.getProperty("whipLen", "12000"));
        kimField.setText(props.getProperty("kim", "0.85"));
        decrToField.setText(props.getProperty("decrSuspProcTo", "50"));
        decrCBox.setSelected(Boolean.parseBoolean(props.getProperty("decrSPTbox", "false")));
        CRBMcomboBox.setSelectedIndex(Integer.parseInt(props.getProperty("CRBMethod", "0")));

        decrToField.setEnabled(decrCBox.isSelected());
    }

    public void initSaveButton(Properties props) {
        saveSettButton.addActionListener(e -> {
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
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Wrong settings format, check settings values");
                return;
            }
            props.setProperty("carRB", carRBfield.getText());
            props.setProperty("suspTG", suspTGfield.getText());
            props.setProperty("suspPT", suspPTfild.getText());
            props.setProperty("shiftDur", shiftDurField.getText());
            props.setProperty("whipLen", whipLenField.getText());
            props.setProperty("kim", kimField.getText());
            props.setProperty("decrSuspProcTo", decrToField.getText());
            props.setProperty("decrSPTbox", decrCBox.isSelected() ? "true" : "false");
            props.setProperty("CRBMethod", String.valueOf(CRBMcomboBox.getSelectedIndex()));

            dispose();
        });
    }

    private void initChkBoxes() {
        decrCBox.addChangeListener(e -> decrToField.setEnabled(decrCBox.isSelected()));
    }
}
