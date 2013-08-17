/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.IrtgParser;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomatonParser;
import java.awt.Component;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.Document;
import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.simplericity.macify.eawt.DefaultApplication;

/**
 *
 * @author koller
 */
public class GuiMain extends javax.swing.JFrame implements ApplicationListener {
    private static File previousDirectory;
    private static GuiMain app;

    static {
        previousDirectory = new File(".");
    }

    /**
     * Creates new form GuiMain
     */
    public GuiMain() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        spLog = new javax.swing.JScrollPane();
        log = new javax.swing.JTextArea();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("IRTG GUI");

        log.setEditable(false);
        log.setColumns(20);
        log.setRows(5);
        spLog.setViewportView(log);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        jMenuItem1.setText("Load IRTG ...");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        jMenuItem2.setText("Load tree automaton ...");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenu1.add(jSeparator1);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        jMenuItem3.setText("Quit");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(spLog, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(spLog, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        if (loadIrtg(this)) {
//            setVisible(false);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        if (loadAutomaton(this)) {
//            setVisible(false);
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    public static void quit() {
        System.exit(0);
    }

    private static File chooseFileForSaving(FileFilter filter, Component parent) {
        JFileChooser fc = new JFileChooser(previousDirectory);
        fc.setFileFilter(filter);

        int returnVal = fc.showSaveDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            previousDirectory = selected.getParentFile();
            return selected;
        } else {
            return null;
        }
    }

    public static boolean saveAutomaton(TreeAutomaton auto, Component parent) {
        File file = chooseFileForSaving(new FileNameExtensionFilter("Tree automata", "auto"), parent);

        try {
            if (file != null) {
                PrintWriter w = new PrintWriter(new FileWriter(file));
                w.println(auto.toString());
                w.close();
                return true;
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while attempting to save automaton as" + file.getName() + ": " + e.getMessage());
        }

        return false;
    }

    private static File chooseFile(FileFilter filter, Component parent) {
        JFileChooser fc = new JFileChooser(previousDirectory);
        fc.setFileFilter(filter);

        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            previousDirectory = selected.getParentFile();
            return selected;
        } else {
            return null;
        }
    }

    public static boolean loadIrtg(Component parent) {
        File file = chooseFile(new FileNameExtensionFilter("IRTG grammars", "irtg"), parent);
        InterpretedTreeAutomaton irtg = null;

        try {
            if (file != null) {
                long start = System.nanoTime();
                irtg = IrtgParser.parse(new FileReader(file));
                log("Loaded IRTG from " + file.getName() + ", " + formatTimeSince(start));
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while attempting to parse " + file.getName() + ": " + e.getMessage());
        }

        if (irtg != null) {
            JTreeAutomaton jta = new JTreeAutomaton(irtg.getAutomaton(), new IrtgTreeAutomatonAnnotator(irtg));
            jta.setTitle("IRTG grammar: " + file.getName());
            jta.setIrtg(irtg);
            jta.setParsingEnabled(true);
            jta.pack();
            jta.setVisible(true);
            return true;
        }

        return false;
    }

    static void showError(Component parent, String error) {
        JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static String formatTimeSince(long start) {
        long end = System.nanoTime();
        if (end - start < 10000) {
            // less than 10 us
            return String.format("%8.3f us", (end - start) / 1000.0);
        } else {
            return String.format("%8.3f ms", (end - start) / 1000000.0);
        }
    }

    public static boolean loadAutomaton(Component parent) {
        File file = chooseFile(new FileNameExtensionFilter("Tree automata", "auto"), parent);
        TreeAutomaton auto = null;

        try {
            if (file != null) {
                long start = System.nanoTime();
                auto = TreeAutomatonParser.parse(new FileReader(file));
                log("Loaded automaton from " + file.getName() + ", " + formatTimeSince(start));
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while attempting to parse " + file.getName() + ": " + e.getMessage());
        }

        if (auto != null) {
            JTreeAutomaton jta = new JTreeAutomaton(auto, null);
            jta.setTitle("Tree automaton: " + file.getName());
            jta.pack();
            jta.setVisible(true);
            return true;
        }

        return false;
    }

    public static void log(final String log) {
        final GuiMain x = app;

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                synchronized (x) {
                    x.log.setText(x.log.getText() + "\n" + log);
                    Document d = x.log.getDocument();
                    x.log.select(d.getLength(), d.getLength());
                }
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) throws Exception {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "IRTG GUI");
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        final GuiMain guiMain = new GuiMain();
        GuiMain.app = guiMain;

        Application application = new DefaultApplication();
        application.addApplicationListener(guiMain);
        application.addPreferencesMenuItem();
        application.setEnabledPreferencesMenu(false);

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                guiMain.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JTextArea log;
    private javax.swing.JScrollPane spLog;
    // End of variables declaration//GEN-END:variables

    /**
     * ** callback methods for macify ***
     */
    public void handleAbout(ApplicationEvent ae) {
    }

    public void handleOpenApplication(ApplicationEvent ae) {
    }

    public void handleOpenFile(ApplicationEvent ae) {
    }

    public void handlePreferences(ApplicationEvent ae) {
    }

    public void handlePrintFile(ApplicationEvent ae) {
    }

    public void handleQuit(ApplicationEvent ae) {
        System.exit(0);
    }

    public void handleReOpenApplication(ApplicationEvent ae) {
    }
}