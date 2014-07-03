/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author koller
 */
public class JDerivationViewer extends javax.swing.JPanel {
    private Interpretation[] interpretationForSelection;
    private Tree<String> derivationTree;
    private static final String INTERPRETATION_PREFIX = "interpretation: ";
    private static final String DERIVATION_TREE = "derivation tree";
    private Map<String, JDerivationDisplayable> displayables;
    private List<String> viewsInOrder;

    /**
     * Creates new form JDerivationViewer
     */
    public JDerivationViewer() {
        initComponents();
        
        viewsInOrder = new ArrayList<String>();        
        viewsInOrder.add("** derivation tree **");
        
        displayables = new HashMap<String, JDerivationDisplayable>();
        displayables.put("** derivation tree **", new JDerivationTree());
        
        String[] possibleViews = new String[1];
        possibleViews[0] = DERIVATION_TREE;
        componentSelector.setModel(new DefaultComboBoxModel(possibleViews));
        
        componentSelector.setSelectedIndex(0);
        
        
    }

    public void setInterpretedTreeAutomaton(InterpretedTreeAutomaton irtg) {
        int N = 1;

        if (irtg != null) {
            N = irtg.getInterpretations().size() + 1;
        }

        displayables = new HashMap<String, JDerivationDisplayable>();
        String[] possibleViews = new String[N];
        interpretationForSelection = new Interpretation[N - 1];

        possibleViews[0] = DERIVATION_TREE;
        possibleViews[0] = DERIVATION_TREE;
        displayables.put("** derivation tree **", new JDerivationTree());

        if (irtg != null) {
            int i = 0;
            for (String name : irtg.getInterpretations().keySet()) {
                interpretationForSelection[i] = irtg.getInterpretations().get(name);
                possibleViews[i + 1] = INTERPRETATION_PREFIX + name;
                displayables.put(name, new JInterpretation(interpretationForSelection[i]));
                viewsInOrder.add(name);
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

    public String getCurrentView() {
        return viewsInOrder.get(componentSelector.getSelectedIndex());
    }
    
    public List<String> getPossibleViews() {
        return viewsInOrder;
    }

    public void setView(String viewName) {
        componentSelector.setSelectedIndex(viewsInOrder.indexOf(viewName));
        changeView(viewName);
    }
    
    private void changeView(String viewName) {
        if (derivationTree != null) {
            content.removeAll();

            JDerivationDisplayable dis = displayables.get(viewName);
            dis.setDerivationTree(derivationTree);
            content.add(dis);

            revalidate();
            repaint();
        }
    }

    private void redraw() {
        setView(getCurrentView());
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
