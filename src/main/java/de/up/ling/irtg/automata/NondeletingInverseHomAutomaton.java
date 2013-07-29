/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import com.google.common.base.Function;
import de.saar.basic.CartesianIterator;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.irtg.signature.Interner;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A tree automaton that describes the homomorphic
 * pre-image of the language of another tree automaton.
 * This class only functions correctly if the homomorphism
 * is non-deleting.
 * 
 * This automaton has the same states as the base automaton,
 * converted into strings.
 * 
 * @author koller
 */
public class NondeletingInverseHomAutomaton<State> extends TreeAutomaton<Object> {
    private TreeAutomaton<State> rhsAutomaton;
    private Homomorphism hom;
//    private Map<String, State> rhsState;
    private int[] labelsRemap; // hom-target(id) = rhs-auto(labelsRemap[id])
    private Function<HomomorphismSymbol,Integer> remappingHomSymbolToIntFunction;

    public NondeletingInverseHomAutomaton(TreeAutomaton<State> rhsAutomaton, Homomorphism hom) {
        super(hom.getSourceSignature());
        this.rhsAutomaton = rhsAutomaton;
        this.hom = hom;
        
        labelsRemap = hom.getTargetSignature().remap(rhsAutomaton.getSignature());
        
        remappingHomSymbolToIntFunction = new Function<HomomorphismSymbol, Integer>() {
            public Integer apply(HomomorphismSymbol f) {
                return labelsRemap[HomomorphismSymbol.getHomSymbolToIntFunction().apply(f)];
            }
        };
        
        this.stateInterner = (Interner) rhsAutomaton.stateInterner;
        allStates = new IntOpenHashSet(rhsAutomaton.getAllStates());
        
        // copying interner of rhsAutomaton is pointless at this point,
        // because rhsAutomaton may be lazy        
//        for( int i = 1; i < rhsAutomaton.stateInterner.getNextIndex(); i++ ) {
//            stateInterner.addObject(rhsAutomaton.stateInterner.resolveId(i).toString());
//        }

        assert hom.isNonDeleting();

//        rhsState = new HashMap<String, State>();

        finalStates.addAll(rhsAutomaton.getFinalStates());
        
//        for (State fin : rhsAutomaton.getFinalStates()) {
//            finalStates.add(fin.toString());
//        }

        // _must_ do this here to cache mapping from strings to rhs states
        // (I think no longer necessary)
        
//        for (State s : rhsAutomaton.getAllStates()) {
//            String normalized = addState(s.toString());
//            rhsState.put(normalized, s);
//        }
    }

    @Override
    public Set<Rule> getRulesBottomUp(int label, final int[] childStates) {
        if (useCachedRuleBottomUp(label, childStates)) {
            return getRulesBottomUpFromExplicit(label, childStates);
        } else {
            Set<Rule> ret = new HashSet<Rule>();

            Set<Integer> resultStates = rhsAutomaton.run(hom.get(label), remappingHomSymbolToIntFunction, new Function<Tree<HomomorphismSymbol>, Integer>() {
                @Override
                public Integer apply(Tree<HomomorphismSymbol> f) {
                    if (f.getLabel().isVariable()) {                      // variable ?i
                        int child = childStates[f.getLabel().getValue()]; // -> i-th child state (= this state ID)
                        return child;                                     // = rhsAuto state ID
//                        return rhsState.get(child);
                    } else {
                        return 0;
                    }
                }
            });

            for (int r : resultStates) {
                // TODO: weight
                Rule rule = createRule(r, label, childStates, 1);
                storeRule(rule);
                ret.add(rule);
            }

            return ret;
        }
    }

    @Override
    public Set<Rule> getRulesTopDown(int label, int parentState) {
        if (useCachedRuleTopDown(label, parentState)) {
            return getRulesTopDownFromExplicit(label, parentState);
        } else {
            Tree<HomomorphismSymbol> rhs = hom.get(label);
            Set<Rule> ret = new HashSet<Rule>();

            for (List<Integer> substitutionTuple : grtdDfs(rhs, parentState, getRhsArity(rhs))) {
                if (isCompleteSubstitutionTuple(substitutionTuple)) {
                    // TODO: weights
                    Rule rule = createRule(parentState, label, substitutionTuple, 1);
                    storeRule(rule);
                    ret.add(rule);
                }
            }

            return ret;
        }
    }
    
    /*
    // note that this breaks the invariant that the state IDs in the interner
    // are a contiguous interval
    protected Rule createRuleI(int parentState, int label, List<Integer> childStates, double weight) {
        return createRuleI(parentState, label, intListToArray(childStates), weight);
    }
    
    protected Rule createRuleI(int parentState, int label, int[] childStates, double weight) {
        stateInterner.addObjectWithIndex(parentState, rhsAutomaton.getStateForId(parentState).toString());
        
        for( int child : childStates ) {
            stateInterner.addObjectWithIndex(child, rhsAutomaton.getStateForId(child).toString());
        }
        
        return super.createRule(parentState, label, childStates, weight);
    }
    */

    private boolean isCompleteSubstitutionTuple(List<Integer> tuple) {
        for (Integer s : tuple) {
            if (s == null) {
                return false;
            }
        }

        return true;
    }

    private int getRhsArity(Tree<HomomorphismSymbol> rhs) {
        int max = -1;

        for (HomomorphismSymbol sym : rhs.getLeafLabels()) {
            if (sym.isVariable() && (sym.getValue() > max)) {
                max = sym.getValue();
            }
        }

        return max + 1;
    }

    private Set<List<Integer>> grtdDfs(Tree<HomomorphismSymbol> rhs, int state, int rhsArity) {
        Set<List<Integer>> ret = new HashSet<List<Integer>>();

        switch (rhs.getLabel().getType()) {
            case CONSTANT:
                for (Rule rhsRule : rhsAutomaton.getRulesTopDown(labelsRemap[rhs.getLabel().getValue()], state)) {
                    List<Set<List<Integer>>> childrenSubstitutions = new ArrayList<Set<List<Integer>>>(); // len = #children

                    for (int i = 0; i < rhsRule.getArity(); i++) {
                        childrenSubstitutions.add(grtdDfs(rhs.getChildren().get(i), rhsRule.getChildren()[i], rhsArity));
                    }

                    CartesianIterator<List<Integer>> it = new CartesianIterator<List<Integer>>(childrenSubstitutions);
                    while (it.hasNext()) {
                        List<List<Integer>> tuples = it.next();  // len = # children x # variables
                        List<Integer> merged = mergeSubstitutions(tuples, rhsArity);
                        if (merged != null) {
                            ret.add(merged);
                        }
                    }
                }
                break;

            case VARIABLE:
                List<Integer> rret = new ArrayList<Integer>(rhsArity);
                int varnum = rhs.getLabel().getValue();

                for (int i = 0; i < rhsArity; i++) {
                    if (i == varnum) {
                        rret.add(state);
                    } else {
                        rret.add(null);
                    }
                }

                ret.add(rret);
        }

//        System.err.println(state + "/" + rhs + "  ==> " + ret);
        return ret;
    }

    // tuples is an n-list of m-lists of output states, where
    // n is number of children, and m is number of variables in homomorphism
    // If n = 0, the method returns [null, ..., null]
    private List<Integer> mergeSubstitutions(List<List<Integer>> tuples, int rhsArity) {
        List<Integer> merged = new ArrayList<Integer>();  // one entry per variable

//        System.err.println("    merge: " + tuples);

        for (int i = 0; i < rhsArity; i++) {
            merged.add(null);
        }

        for (int i = 0; i < tuples.size(); i++) {
            for (int j = 0; j < rhsArity; j++) {
                Integer state = tuples.get(i).get(j);
                if (state != null) {
                    if (merged.get(j) != null && !merged.get(j).equals(state)) {
                        return null;
                    } else {
                        merged.set(j, state);
                    }
                }
            }
        }

//        System.err.println("    --> merged: " + merged);

        return merged;
    }

    @Override
    public boolean isBottomUpDeterministic() {
        return rhsAutomaton.isBottomUpDeterministic();
    }
}
