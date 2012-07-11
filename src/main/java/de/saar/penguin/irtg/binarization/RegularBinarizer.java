/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import de.saar.basic.StringOrVariable;
import de.saar.penguin.irtg.algebra.Algebra;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author koller
 */
public abstract class RegularBinarizer<E> {
    public static final String VARIABLE_MARKER = "_"; // terminals representing variables start with this
    protected Algebra inputAlgebra, outputAlgebra;
    private int nextGensym;

    public RegularBinarizer(Algebra inputAlgebra, Algebra outputAlgebra) {
        this.inputAlgebra = inputAlgebra;
        this.outputAlgebra = outputAlgebra;
        nextGensym = 1;
    }

    public abstract TreeAutomaton<E> binarize(String symbol, int arity);

    public TreeAutomaton<String> binarize(Tree<String> term) {
        return null;
    }

    public TreeAutomaton<String> binarizeWithVariables(Tree<StringOrVariable> term) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        String finalState = gensym();
        ret.addFinalState(finalState);
        binarizeWithVariablesInto(term, finalState, ret);
        return ret;
    }

    private void binarizeWithVariablesInto(Tree<StringOrVariable> term, String renameFinalStatesTo, ConcreteTreeAutomaton<String> intoAutomaton) {
        if (term.getLabel().isVariable()) {
            intoAutomaton.addRule(term.getLabel().getValue(), new ArrayList<String>(), renameFinalStatesTo);
        } else {
            TreeAutomaton<E> auto = binarize(term.getLabel().getValue(), term.getChildren().size());
            List<String> variableStates = copyWithRenaming(auto, auto.getFinalStates(), renameFinalStatesTo, intoAutomaton);

            for (int i = 0; i < term.getChildren().size(); i++) {
                binarizeWithVariablesInto(term.getChildren().get(i), variableStates.get(i), intoAutomaton);
            }
        }
    }

    private List<String> copyWithRenaming(TreeAutomaton<E> automaton, Set<E> statesToRename, String newStateName, ConcreteTreeAutomaton<String> intoAutomaton) {
        Map<E, String> stateNameMap = new HashMap<E, String>();
        Map<String, E> variableToState = new HashMap<String, E>();

        // initialize state map with given renaming instructions
        for (E stateToRename : statesToRename) {
            stateNameMap.put(stateToRename, newStateName);
        }

        // go through all rules and copy while renaming
        for (Rule<E> rule : automaton.getRuleSet()) {
            if (rule.getLabel().startsWith(VARIABLE_MARKER)) {
                // rules whose terminal symbols are variables:
                // don't copy, but instead store variable name and state
                variableToState.put(rule.getLabel(), rule.getParent());
            } else {
                // other rules; rename states and copy rule into other automaton
                String[] newChildren = new String[rule.getChildren().length];
                for (int i = 0; i < rule.getChildren().length; i++) {
                    newChildren[i] = getOrGenState(rule.getChildren()[i], stateNameMap);
                }

                Rule<String> newRule = new Rule<String>(getOrGenState(rule.getParent(), stateNameMap), rule.getLabel(), newChildren);
                intoAutomaton.addRule(newRule);
            }
        }

        // collect list of variable states
        List<String> variableNames = new ArrayList<String>(variableToState.keySet());
        Collections.sort(variableNames);

        List<String> ret = new ArrayList<String>(variableNames.size());
        for (String var : variableNames) {
            ret.add(stateNameMap.get(variableToState.get(var)));
        }

        return ret;
    }

    private String getOrGenState(E state, Map<E, String> stateMap) {
        if (stateMap.containsKey(state)) {
            return stateMap.get(state);
        } else {
            String ret = gensym();
            stateMap.put(state, ret);
            return ret;
        }
    }

    private String gensym() {
        return "q" + (nextGensym++);
    }

    public Algebra getInputAlgebra() {
        return inputAlgebra;
    }

    public Algebra getOutputAlgebra() {
        return outputAlgebra;
    }
}
