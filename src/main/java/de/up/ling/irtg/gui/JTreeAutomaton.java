/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.saar.basic.StringTools;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class JTreeAutomaton extends javax.swing.JFrame {
    private TreeAutomaton automaton;
    private InterpretedTreeAutomaton irtg;
    List<String> annotationsInOrder;

    /**
     * Creates new form JInterpretedTreeAutomaton
     */
    public JTreeAutomaton(TreeAutomaton<?> automaton, TreeAutomatonAnnotator annotator) {
        initComponents();

        this.automaton = automaton;

        Vector<String> columnIdentifiers = new Vector<String>();
        columnIdentifiers.add("");
        columnIdentifiers.add("");
        columnIdentifiers.add("");

        annotationsInOrder = new ArrayList<String>();
        if (annotator != null) {
            annotationsInOrder.addAll(annotator.getAnnotationIdentifiers());
            columnIdentifiers.addAll(annotationsInOrder);
        }

        entries.setColumnIdentifiers(columnIdentifiers);

        for (Rule rule : automaton.getRuleSet()) {
            Vector<String> row = new Vector<String>();
            row.add(automaton.getStateForId(rule.getParent()).toString() + (automaton.getFinalStates().contains(rule.getParent()) ? "!" : ""));
            row.add("->");

            List<String> resolvedRhsStates = new ArrayList<String>();
            for (int childState : rule.getChildren()) {
                resolvedRhsStates.add(automaton.getStateForId(childState).toString());
            }

            String label = automaton.getSignature().resolveSymbolId(rule.getLabel());
            row.add(label
                    + (rule.getArity() > 0 ? "(" : "")
                    + StringTools.join(resolvedRhsStates, ", ")
                    + (rule.getArity() > 0 ? ")" : ""));

            if (annotator != null) {
                for (String anno : annotationsInOrder) {
                    row.add(annotator.getAnnotation(rule, anno));
                }
            }

            entries.addRow(row);
        }

        TableColumnAdjuster tca = new TableColumnAdjuster(jTable1);
//        tca.setOnlyAdjustLarger(false);
        tca.adjustColumns();
    }

    public void setIrtg(InterpretedTreeAutomaton irtg) {
        this.irtg = irtg;

    }

    public void setParsingEnabled(boolean enabled) {
        miParse.setEnabled(enabled);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        entries = new javax.swing.table.DefaultTableModel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        miOpenIrtg = new javax.swing.JMenuItem();
        miOpenAutomaton = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        miSaveAutomaton = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        miQuit = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        miShowLanguage = new javax.swing.JMenuItem();
        miParse = new javax.swing.JMenuItem();

        jMenu1.setText("File");
        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");
        jMenuBar1.add(jMenu2);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jTable1.setModel(entries);
        jTable1.setAutoCreateRowSorter(true);
        jScrollPane1.setViewportView(jTable1);

        jMenu3.setText("File");

        miOpenIrtg.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.META_MASK));
        miOpenIrtg.setText("Open IRTG ...");
        miOpenIrtg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenIrtgActionPerformed(evt);
            }
        });
        jMenu3.add(miOpenIrtg);

        miOpenAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.META_MASK));
        miOpenAutomaton.setText("Open tree automaton ...");
        miOpenAutomaton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenAutomatonActionPerformed(evt);
            }
        });
        jMenu3.add(miOpenAutomaton);
        jMenu3.add(jSeparator1);

        miSaveAutomaton.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.META_MASK));
        miSaveAutomaton.setText("Save tree automaton ...");
        miSaveAutomaton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveAutomatonActionPerformed(evt);
            }
        });
        jMenu3.add(miSaveAutomaton);
        jMenu3.add(jSeparator2);

        miQuit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.META_MASK));
        miQuit.setText("Quit");
        miQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miQuitActionPerformed(evt);
            }
        });
        jMenu3.add(miQuit);

        jMenuBar2.add(jMenu3);

        jMenu4.setText("Tools");

        miShowLanguage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.META_MASK));
        miShowLanguage.setText("Show language ...");
        miShowLanguage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowLanguageActionPerformed(evt);
            }
        });
        jMenu4.add(miShowLanguage);

        miParse.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.META_MASK));
        miParse.setText("Parse ...");
        miParse.setEnabled(false);
        miParse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miParseActionPerformed(evt);
            }
        });
        jMenu4.add(miParse);

        jMenuBar2.add(jMenu4);

        setJMenuBar(jMenuBar2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 388, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void miOpenIrtgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenIrtgActionPerformed
        GuiMain.loadIrtg(this);
    }//GEN-LAST:event_miOpenIrtgActionPerformed

    private void miOpenAutomatonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenAutomatonActionPerformed
        GuiMain.loadAutomaton(this);
    }//GEN-LAST:event_miOpenAutomatonActionPerformed

    private void miSaveAutomatonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miSaveAutomatonActionPerformed
        GuiMain.saveAutomaton(automaton, this);
    }//GEN-LAST:event_miSaveAutomatonActionPerformed

    private void miQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miQuitActionPerformed
        GuiMain.quit();
    }//GEN-LAST:event_miQuitActionPerformed

    private void miShowLanguageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miShowLanguageActionPerformed
        JLanguageViewer lv = new JLanguageViewer();
        lv.setAutomaton(automaton, irtg);
        lv.pack();
        lv.setVisible(true);
    }//GEN-LAST:event_miShowLanguageActionPerformed

    private void miParseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miParseActionPerformed
        if (irtg != null) {
            Map<String, String> inputs = JInputForm.getValues(annotationsInOrder, this);

            if (inputs != null) {
                TreeAutomaton chart = null;

                try {
                    chart = irtg.parse(inputs);
                } catch (ParserException ex) {
                    GuiMain.showError(this, "An error occurred while parsing the input objects: " + inputs);
                }
                
                if( chart != null ) {
                    JLanguageViewer jlv = new JLanguageViewer();
                    jlv.setAutomaton(chart, irtg);
                    jlv.pack();
                    jlv.setVisible(true);
                }
            }

        }
    }//GEN-LAST:event_miParseActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.table.DefaultTableModel entries;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTable jTable1;
    private javax.swing.JMenuItem miOpenAutomaton;
    private javax.swing.JMenuItem miOpenIrtg;
    private javax.swing.JMenuItem miParse;
    private javax.swing.JMenuItem miQuit;
    private javax.swing.JMenuItem miSaveAutomaton;
    private javax.swing.JMenuItem miShowLanguage;
    // End of variables declaration//GEN-END:variables
}
