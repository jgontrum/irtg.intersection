/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.tree.Tree;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;

/**
 *
 * @author koller
 */
public class JDerivationViewer extends javax.swing.JPanel {
    private Interpretation[] interpretationForSelection;
    private JComponent[] components;
    private Tree<String> derivationTree;
    private static final String INTERPRETATION_PREFIX = "interpretation: ";
    private static final String DERIVATION_TREE = "derivation tree";

    /**
     * Creates new form JDerivationViewer
     */
    public JDerivationViewer() {
        initComponents();
    }

    public void setInterpretedTreeAutomaton(InterpretedTreeAutomaton irtg) {
        int N = 1;

        if (irtg != null) {
            N = irtg.getInterpretations().size() + 1;
        }

        String[] possibleViews = new String[N];
        interpretationForSelection = new Interpretation[N - 1];
        components = new JComponent[N];

        possibleViews[0] = DERIVATION_TREE;
        components[0] = new JDerivationTree();

        if (irtg != null) {
            int i = 0;
            for (String name : irtg.getInterpretations().keySet()) {
                interpretationForSelection[i] = irtg.getInterpretations().get(name);
                possibleViews[i + 1] = INTERPRETATION_PREFIX + name;
                components[i + 1] = new JInterpretation(interpretationForSelection[i]);
                i++;
            }
        }

        componentSelector.setModel(new DefaultComboBoxModel(possibleViews));
        derivationTree = null;
    }

    public void displayDerivation(Tree<String> derivationTree) {
        this.derivationTree = derivationTree;
        redraw();
    }

    private void redraw() {
        if (derivationTree != null) {
            int index = componentSelector.getSelectedIndex();

            content.removeAll();

            if (index == 0) {
                JDerivationTree jdt = (JDerivationTree) components[0];
                jdt.setDerivationTree(derivationTree);
                content.add(jdt);
            } else {
                JInterpretation ji = (JInterpretation) components[index];
                ji.setDerivationTree(derivationTree);
                content.add(ji);
            }

            revalidate();
            repaint();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        componentSelector = new javax.swing.JComboBox();
        content = new javax.swing.JPanel();

        jLabel1.setText("View:");

        componentSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                componentSelectorActionPerformed(evt);
            }
        });

        content.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(content, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(jLabel1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(componentSelector, 0, 443, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel1)
                    .add(componentSelector, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(content, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void componentSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_componentSelectorActionPerformed
        redraw();
    }//GEN-LAST:event_componentSelectorActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox componentSelector;
    private javax.swing.JPanel content;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables
}
