/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import com.bric.window.WindowList;
import com.bric.window.WindowMenu;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomatonParser;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.corpus.Charts;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.FileInputStreamSupplier;
import de.up.ling.irtg.maxent.MaximumEntropyIrtg;
import de.up.ling.irtg.util.Util;
import java.awt.Component;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
    private static ExecutorService executorService = Executors.newFixedThreadPool(1);

    static {
        previousDirectory = new File(".");
    }

    /**
     * Creates new form GuiMain
     */
    public GuiMain() {
        initComponents();
        jMenuBar1.add(new WindowMenu(this));
    }

    public static GuiMain getApplication() {
        return app;
    }

    public static boolean isMac() {
        return new DefaultApplication().isMac();
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
        jMenuItem4 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();

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
        jMenuItem2.setText("Load Tree Automaton ...");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);
        jMenu1.add(jSeparator1);

        jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        jMenuItem4.setText("Close All Windows");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem4);
        jMenu1.add(jSeparator2);

        jMenuItem3.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        jMenuItem3.setText("Quit");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem3);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Tools");

        jMenuItem5.setText("Compute decomposition automaton ...");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem5);

        jMenuBar1.add(jMenu2);

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

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        closeAllWindows();
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        showDecompositionDialog(this);
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    public static void showDecompositionDialog(java.awt.Frame parent) {
        new DecompositionDialog(parent, true).setVisible(true);
    }

    public static void quit() {
        System.exit(0);
    }

    public static void closeAllWindows() {
        for (Window window : WindowList.getWindows(false, false)) {
            if (!(window instanceof GuiMain)) {
                window.setVisible(false);
            }
        }
    }

    public static File chooseFileForSaving(FileFilter filter, Component parent) {
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

    public static Corpus loadAnnotatedCorpus(InterpretedTreeAutomaton irtg, Component parent) {
        File file = chooseFile("Open annotated corpus", new FileNameExtensionFilter("Annotated corpora (*.txt)", "txt"), parent);

        try {
            if (file != null) {
                long start = System.nanoTime();
                Corpus corpus = irtg.readCorpus(new FileReader(file));
                log("Read annotated corpus from " + file.getName() + ", " + formatTimeSince(start));

                if (!corpus.isAnnotated()) {
                    showError(parent, "The file " + file.getName() + " is not an annotated corpus.");
                    return null;
                }

                return corpus;
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while reading the corpus " + file.getName() + ": " + e.getMessage());
        }

        return null;
    }

    private static String stripExtension(String filename) {
        Pattern p = Pattern.compile("(.*)\\.[^.]*");
        Matcher m = p.matcher(filename);

        if (m.matches()) {
            return m.group(1);
        } else {
            return filename;
        }
    }

    public static void loadMaxentWeights(final MaximumEntropyIrtg irtg, final JFrame parent) {
        final File file = chooseFile("Open maxent weights", new FileNameExtensionFilter("Maxent weights (*.txt)", "txt"), parent);

        try {
            if (file != null) {
                long start = System.nanoTime();
                irtg.readWeights(new FileReader(file));

                log("Read maximum entropy weights from " + file.getName() + ", " + formatTimeSince(start));
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while reading the maxent weights file " + file.getName() + ": " + e.getMessage());
        }
    }

    public static Corpus loadUnannotatedCorpus(final InterpretedTreeAutomaton irtg, final JFrame parent) {
        final File file = chooseFile("Open unannotated corpus", new FileNameExtensionFilter("Unannotated corpora (*.txt)", "txt"), parent);
        ChartComputationProgressBar pb = null;
        FileOutputStream fos = null;

        try {
            if (file != null) {
                long start = System.nanoTime();
                final Corpus corpus = irtg.readCorpus(new FileReader(file));
                log("Read unannotated corpus from " + file.getName() + ", " + formatTimeSince(start));

                File chartsFile = chooseFile("Open precomputed parse charts (or cancel)", new FileNameExtensionFilter("Parse charts (*.zip)", "zip"), parent);

                // if user didn't select precomputed charts, compute them now
                if (chartsFile == null) {
                    chartsFile = new File(file.getParent(), stripExtension(file.getName()) + "-charts.zip");
                    fos = new FileOutputStream(chartsFile);

                    pb = new ChartComputationProgressBar(parent, false, corpus.getNumberOfInstances());
                    pb.setVisible(true);
                    start = System.nanoTime();
                    Charts.computeCharts(corpus, irtg, fos, pb);
                    log("Wrote parse charts to " + chartsFile + ", " + formatTimeSince(start));
                    pb.setVisible(false);
                }

                Charts charts = new Charts(new FileInputStreamSupplier(chartsFile));
                corpus.attachCharts(charts);

                return corpus;
            }
        } catch (Exception e) {
            if (pb != null) {
                pb.setVisible(false);
            }

            showError(parent, "An error occurred while reading the corpus " + file.getName() + ": " + e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                }
            }
        }

        return null;
    }

    private static File chooseFile(String title, FileFilter filter, Component parent) {
        List<FileFilter> f = new ArrayList<>();
        f.add(filter);
        return chooseFile(title, f, parent);
    }

    private static File chooseFile(String title, List<FileFilter> filters, Component parent) {
        JFileChooser fc = new JFileChooser(previousDirectory);
        fc.setDialogTitle(title);

        for (FileFilter f : filters) {
            fc.addChoosableFileFilter(f);
        }

        int returnVal = fc.showOpenDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            previousDirectory = selected.getParentFile();
            return selected;
        } else {
            return null;
        }
    }

    private static class InputCodecFileFilter extends FileFilter {

        private InputCodec ic;

        public InputCodecFileFilter(InputCodec ic) {
            this.ic = ic;
        }

        @Override
        public boolean accept(File f) {
            return f.getName().endsWith(ic.getMetadata().extension()) || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return ic.getMetadata().description() + " (*." + ic.getMetadata().extension() + ")";
        }

    }

    private static class LoadingResult<T> {

        T object;
        File filename;
        String readingTime;

        public LoadingResult(T object, File filename, String readingTime) {
            this.object = object;
            this.filename = filename;
            this.readingTime = readingTime;
        }

    }

    private static <T> LoadingResult<T> loadObject(Class<T> objectClass, String objectDescription, Component parent) {
        List<FileFilter> filters = new ArrayList<>();

        for (InputCodec ic : InputCodec.getInputCodecs(objectClass)) {
            filters.add(new InputCodecFileFilter(ic));
        }

        File file = chooseFile("Open " + objectDescription, filters, parent);

        try {
            if (file != null) {
                long start = System.nanoTime();

                String ext = Util.getFilenameExtension(file.getName());
                InputCodec<T> codec = InputCodec.getInputCodecByExtension(ext);

                if (codec == null) {
                    showError(parent, "Could not identify input codec for file extension '" + ext + "'");
                } else if (codec.getMetadata().type() != objectClass) {
                    showError(parent, "The codec '" + codec.getMetadata().name() + "' is not suitable for reading a" + objectDescription + ".");
                } else {
                    T obj = codec.read(new FileInputStream(file));
                    String time = formatTimeSince(start);
                    return new LoadingResult<>(obj, file, time);
                }
            }
        } catch (Exception e) {
            showError(parent, "An error occurred while attempting to parse " + file.getName() + ": " + e.getMessage());
        }

        return null;
    }

    public static boolean loadIrtg(Component parent) {
        LoadingResult<InterpretedTreeAutomaton> result = loadObject(InterpretedTreeAutomaton.class, "IRTG", parent);

        if (result != null) {
            log("Loaded IRTG from " + result.filename.getName() + ", " + result.readingTime);

            InterpretedTreeAutomaton irtg = result.object;

            JTreeAutomaton jta = new JTreeAutomaton(irtg.getAutomaton(), new IrtgTreeAutomatonAnnotator(irtg));
            jta.setTitle("IRTG grammar: " + result.filename.getName());
            jta.setIrtg(irtg);
            jta.setParsingEnabled(true);
            jta.pack();
            jta.setVisible(true);
            return true;
        }

        return false;
    }

    static public void showError(Component parent, String error) {
        JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static String formatTimeSince(long start) {
        long end = System.nanoTime();
        if (end - start < 1000) {
            return (end - start) + " ns";
        } else if (end - start < 1000000) {
            return (end - start) / 1000 + " \u03bcs";
        } else if (end - start < 1000000000) {
            return (end - start) / 1000000 + " ms";
        } else {
            StringBuffer buf = new StringBuffer();
            long diff = end - start;

            if (diff > 60000000000L) {
                buf.append(diff / 60000000000L + "m ");
            }

            diff %= 60000000000L;

            buf.append(String.format("%d.%03ds", diff / 1000000000, (diff % 1000000000) / 1000000));
            return buf.toString();
        }
    }

    public static boolean loadAutomaton(Component parent) {
        LoadingResult<TreeAutomaton> result = loadObject(TreeAutomaton.class, "tree automaton", parent);

        if (result != null) {
            log("Loaded tree automaton from " + result.filename.getName() + ", " + result.readingTime);
            TreeAutomaton auto = result.object;
            
            JTreeAutomaton jta = new JTreeAutomaton(auto, null);
            jta.setTitle("Tree automaton: " + result.filename.getName());
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
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
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
