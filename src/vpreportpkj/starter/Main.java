package vpreportpkj.starter;

import vpreportpkj.ui.MainForm;

import javax.swing.*;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        try {
            MainForm mf = new MainForm();
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Unexpected exception");
        }
    }
}
