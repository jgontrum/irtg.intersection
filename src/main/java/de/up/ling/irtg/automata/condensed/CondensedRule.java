/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gontrum
 */
public class CondensedRule {

    private int labelSetID;
    private int parent;
    private int[] children;
    private double weight;
    private Object extra;

    public CondensedRule(int parent, int labelSetID, int[] children, double weight) {
        this.parent = parent;
        this.labelSetID = labelSetID;
        this.children = children;
        this.weight = weight;
    }

    public int[] getChildren() {
        return children;
    }

    public int getLabelSetID() {
        return labelSetID;
    }

    public IntSet getLabels(CondensedTreeAutomaton auto) {
        return auto.getLabelsForID(labelSetID);
    }

    /**
     * Returns a Set of Strings for the labels of this rule.
     *
     * @param auto
     * @return
     */
    public Collection<String> getLabelStrings(CondensedTreeAutomaton auto) {
        Set<String> ret = new HashSet<String>();
        for (int label : getLabels(auto)) {
            ret.add(auto.getSignature().resolveSymbolId(label));
        }
        return ret;
    }

    public int getParent() {
        return parent;
    }

    // TODO - is this needed?
    public void setParent(int parent) {
        this.parent = parent;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * Retrieves the auxiliary information from this rule.
     *
     * @see #setExtra(java.lang.Object)
     * @return
     */
    public Object getExtra() {
        return extra;
    }

    /**
     * Stores auxiliary information within this rule. Do not use this unless you
     * know what you're doing.
     *
     * @param extra
     */
    public void setExtra(Object extra) {
        this.extra = extra;
    }

    public int getArity() {
        return children.length;
    }

    @Override
    public String toString() {
        boolean first = true;
        StringBuilder ret = new StringBuilder(parent + " -> %" + labelSetID + "%");
        if (children.length > 0) {
            ret.append("(");

            for (int child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                ret.append(child);
            }

            ret.append(")");
        }

        ret.append(" [" + weight + "]");
        return ret.toString();
    }

    public String toString(CondensedTreeAutomaton auto) {
        return toString(auto, auto.getFinalStates().contains(parent));
    }

    public String toString(CondensedTreeAutomaton auto, boolean parentIsFinal) {
        return toString(auto, parentIsFinal, -1);
    }

    public String toString(CondensedTreeAutomaton auto, int dotPosition) {
        return toString(auto, auto.getFinalStates().contains(parent), dotPosition);
    }

    public String toString(CondensedTreeAutomaton auto, boolean parentIsFinal, int dotPosition) {
        boolean first = true;
        StringBuilder ret = new StringBuilder(Tree.encodeLabel(auto.getStateForId(parent).toString()) + (parentIsFinal ? "!" : "") + " -> {");

        Collection<String> labels = getLabelStrings(auto);
        if (labels.size() > 5) {
            ret.append("[" + labels.size() + " labels]");
        } else {
            for (String label : getLabelStrings(auto)) {
                ret.append(label + ",");
            }
        }

        if (!getLabelStrings(auto).isEmpty()) {
            ret.deleteCharAt(ret.length() - 1);
        }

        ret.append("}");
        if (children.length > 0) {
            ret.append("(");

            for (int child : children) {
                if (first) {
                    first = false;
                } else {
                    ret.append(", ");
                }

                if (dotPosition == child) {
                    ret.append("* ");
                }

                ret.append((child == 0) ? "null" : Tree.encodeLabel(auto.getStateForId(child).toString()));
            }

            if (dotPosition == children.length) {
                ret.append("* ");
            }

            ret.append(")");
        }

        ret.append(" [" + weight + "]");
        return ret.toString();
    }

}
