/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.gui;

import de.up.ling.tree.Tree;
import de.up.ling.tree.TreePanel;
import java.awt.Color;
import javax.swing.JScrollPane;

/**
 *
 * @author koller
 */
public class JDerivationTree extends javax.swing.JPanel {
    /**
     * Creates new form JDerivationTre
     */
    public JDerivationTree() {
        initComponents();
    }
    
    public void setDerivationTree(Tree<String> derivationTree) {
        removeAll();
        JScrollPane jsp = new JScrollPane(new TreePanel(derivationTree), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jsp.setBackground(Color.white);
        add(jsp);        
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBorder(javax.swing.BorderFactory.createTitledBorder("Derivation tree"));
        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
