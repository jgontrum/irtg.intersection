/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.gui.popup.PopupMenu;
import de.up.ling.irtg.gui.popup.PopupTextSource;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import java.awt.Color;
import java.util.Arrays;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;

/**
 *
 * @author koller
 */
public class JInterpretation extends JDerivationDisplayable {
    private Interpretation interp;

    /**
     * Creates new form JInterpretation
     */
    public JInterpretation(Interpretation interp) {
        this.interp = interp;

        initComponents();


    }

    private JComponent makeErrorComponent(Exception e) {
        JLabel ret = new JLabel("<Can't evaluate: " + e.toString() + ">");

        String tooltipText = "<html>" + e + "<br>" + StringTools.join(Arrays.asList(e.getStackTrace()), "<br>\n") + "</html>";

        ret.setToolTipText(tooltipText);
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        return ret;
    }

    @Override
    public void setDerivationTree(Tree<String> derivationTree) {
        final Tree<String> term = interp.getHomomorphism().apply(derivationTree);

        termPanel.removeAll();
        JComponent treePanel = sp(new TreePanel(term));
        termPanel.add(treePanel);

        PopupMenu termMenu = new PopupMenu();
        termMenu.setTextSource(new PopupTextSource() {
            public String getText() {
                return term.toString();
            }
        });
        termMenu.addAsMouseListener(treePanel);

        valuePanel.removeAll();

        JComponent valueComponent = null;

        try {
            final Object value = interp.getAlgebra().evaluate(term);
            valueComponent = interp.getAlgebra().visualize(value);

            PopupMenu valueMenu = new PopupMenu();
            valueMenu.setTextSource(new PopupTextSource() {
                public String getText() {
                    return value.toString();
                }
            });
            valueMenu.addAsMouseListener(valueComponent);
        } catch (Exception e) {
            valueComponent = makeErrorComponent(e);
        }

        valuePanel.add(sp(valueComponent));
    }

    private JComponent sp(JComponent comp) {
        JScrollPane ret = new JScrollPane(comp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ret.setBackground(Color.white);
        return ret;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        termPanel = new javax.swing.JPanel();
        valuePanel = new javax.swing.JPanel();

        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        termPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Term"));
        termPanel.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setBottomComponent(termPanel);

        valuePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Value"));
        valuePanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                handleValuePanelResized(evt);
            }
        });
        valuePanel.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setLeftComponent(valuePanel);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void handleValuePanelResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_handleValuePanelResized
        valuePanel.setPreferredSize(valuePanel.getSize());
//        valuePanel.setm
    }//GEN-LAST:event_handleValuePanelResized
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel termPanel;
    private javax.swing.JPanel valuePanel;
    // End of variables declaration//GEN-END:variables
}
