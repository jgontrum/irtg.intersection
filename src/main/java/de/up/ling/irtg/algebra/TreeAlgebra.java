/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class TreeAlgebra implements Algebra<Tree<String>> {
    protected final Signature signature = new Signature();

    @Override
    public Tree<String> evaluate(Tree<Integer> t) {
        return signature.resolve(t);
    }

    @Override
    public TreeAutomaton decompose(Tree<String> value) {
        return new TreeDecomposingAutomaton(value);
    }

    @Override
    public Signature getSignature() {
        return signature;
    }

    private class TreeDecomposingAutomaton extends TreeAutomaton<String> {
        private Tree<Integer> tree;
        private final Set<Integer> labels;
        private ListMultimap<Integer, String> leafLabelsToStates;

        public TreeDecomposingAutomaton(Tree<String> derivedTree) {
            super(TreeAlgebra.this.getSignature());

            this.tree = TreeAlgebra.this.getSignature().addAllSymbols(derivedTree);

            labels = new HashSet<Integer>();
            leafLabelsToStates = ArrayListMultimap.create();

            collectStatesAndLabels(tree, "q");
            finalStates.add("q");
        }

        @Override
        public Set<Rule<String>> getRulesBottomUp(int label, List<String> childStates) {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();

            if (childStates.isEmpty()) {
                for (String state : leafLabelsToStates.get(label)) {
                    Rule<String> rule = createRule(state, label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            } else {
                String potentialParent = childStates.get(0).substring(0, childStates.get(0).length() - 1);
                boolean correctChildren = true;

                for (int i = 0; i < childStates.size(); i++) {
                    if (!childStates.get(i).equals(potentialParent + i)) {
                        correctChildren = false;
                    }
                }

                if (correctChildren && tree.select(potentialParent, 1).getLabel().equals(label)) {
                    Rule<String> rule = createRule(potentialParent, label, childStates);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }

        @Override
        public Set<Rule<String>> getRulesTopDown(int label, String parentState) {
            Set<Rule<String>> ret = new HashSet<Rule<String>>();
            Tree<Integer> t = tree.select(parentState, 1);

            if (t.getLabel().equals(label)) {
                List<String> children = new ArrayList<String>();
                for (int i = 0; i < t.getChildren().size(); i++) {
                    children.add(parentState + i);
                }

                Rule<String> rule = createRule(parentState, label, children);
                ret.add(rule);
                storeRule(rule);
            }


            return ret;
        }

        private void collectStatesAndLabels(Tree<Integer> node, String state) {
            state = addState(state);
            labels.add(node.getLabel());

            if (node.getChildren().isEmpty()) {
                leafLabelsToStates.put(node.getLabel(), state);
            }

            for (int i = 0; i < node.getChildren().size(); i++) {
                collectStatesAndLabels(node.getChildren().get(i), state + i);
            }
        }

        @Override
        public boolean isBottomUpDeterministic() {
            return true;
        }
    }

    @Override
    public Tree<String> parseString(String representation) throws ParserException {
        Tree<String> ret = TreeParser.parse(representation);
        signature.addAllSymbols(ret);
        return ret;
    }
    
    /*  ** unused? **
    public Tree<StringOrVariable> parseStringWithVariables(String representation) {
        Tree<StringOrVariable> ret = TermParser.parse(representation).toTreeWithVariables();
        signature.addAllConstants(ret);
        return ret;
    }
    */
}
