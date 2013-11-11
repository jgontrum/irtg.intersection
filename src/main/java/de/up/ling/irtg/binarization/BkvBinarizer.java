/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.binarization;

import com.google.common.base.Function;
import de.saar.basic.StringTools;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Implements the binarization algorithm of Buechse/Koller/Vogler.
 *
 * @author koller
 */
public class BkvBinarizer {
    private Map<String, RegularSeed> regularSeeds;
    private int nextGensym = 0;

    public BkvBinarizer(Map<String, RegularSeed> regularSeeds) {
        this.regularSeeds = regularSeeds;
    }
    
    public InterpretedTreeAutomaton binarize(InterpretedTreeAutomaton irtg) {
        ConcreteTreeAutomaton<String> binarizedRtg = new ConcreteTreeAutomaton<String>();
        Map<String, Homomorphism> binarizedHom = new HashMap<String, Homomorphism>();
        List<String> interpretationNames = new ArrayList<String>(irtg.getInterpretations().keySet());
        TreeAutomaton rtg = irtg.getAutomaton();

        // initialize output homs
        for (String interp : interpretationNames) {
            Homomorphism oldHom = irtg.getInterpretations().get(interp).getHomomorphism();
            binarizedHom.put(interp, new Homomorphism(oldHom.getSourceSignature(), oldHom.getTargetSignature()));
        }

        for (Rule rule : irtg.getAutomaton().getRuleSet()) {
            if (rule.getArity() <= 2) {
                // rules of low arity => simply copy these to result
                copyRule(rule, binarizedRtg, binarizedHom, irtg);
            } else {
                // rules of higher arity => binarize
                RuleBinarization rb = binarizeRule(rule, irtg);

                if (rb == null) {
                    // unbinarizable => copy to result
                    copyRule(rule, binarizedRtg, binarizedHom, irtg);
                } else {
                    // else, add binarized rule to result
                    String[] childStates = new String[rule.getArity()];
                    for (int i = 0; i < rule.getArity(); i++) {
                        childStates[i] = rtg.getStateForId(rule.getChildren()[i]).toString();
                    }

                    Object parent = rtg.getStateForId(rule.getParent());
                    addRulesToAutomaton(binarizedRtg, rb.xi, parent.toString(), childStates);

                    if (rtg.getFinalStates().contains(parent)) {
                        binarizedRtg.addFinalState(binarizedRtg.getIdForState(parent.toString()));
                    }

                    for (String interp : interpretationNames) {
                        addEntriesToHomomorphism(binarizedHom.get(interp), rb.xi, rb.binarizationTerms.get(interp));  // FISHY
                    }
                }
            }
        }

        // assemble output IRTG
        InterpretedTreeAutomaton ret = new InterpretedTreeAutomaton(binarizedRtg);
        for (String interp : interpretationNames) {
            ret.addInterpretation(interp, new Interpretation(irtg.getInterpretations().get(interp).getAlgebra(), binarizedHom.get(interp)));
        }

        return ret;
    }

    private void copyRule(Rule rule, ConcreteTreeAutomaton<String> binarizedRtg, Map<String, Homomorphism> binarizedHom, InterpretedTreeAutomaton irtg) {
        binarizedRtg.addRule(transferRule(rule, irtg.getAutomaton(), binarizedRtg));

        for (String interp : irtg.getInterpretations().keySet()) {
            binarizedHom.get(interp).add(rule.getLabel(), irtg.getInterpretations().get(interp).getHomomorphism().get(rule.getLabel()));
        }
    }

    // inserts rules with fresh states into the binarized RTG
    // for generating q -> xi(q1,...,qk)
    private void addRulesToAutomaton(final ConcreteTreeAutomaton binarizedRtg, final Tree<String> xi, final String oldRuleParent, final String[] oldRuleChildren) {
        xi.dfs(new TreeVisitor<String, Void, String>() {
            @Override
            public String combine(Tree<String> node, List<String> childrenValues) {
                if (node.getLabel().startsWith("?")) {
                    assert childrenValues.isEmpty();
                    int var = Integer.parseInt(node.getLabel().substring(1)) - 1;
                    return oldRuleChildren[var];
                } else {
                    String parent;

                    if (node == xi) {
                        parent = oldRuleParent;
                    } else {
                        parent = gensym("q");
                    }

                    binarizedRtg.addRule(binarizedRtg.createRule(parent, node.getLabel(), childrenValues));
                    return parent;
                }
            }
        });
    }

    RuleBinarization binarizeRule(Rule rule, InterpretedTreeAutomaton irtg) {
        TreeAutomaton commonVariableTrees = null;
        Map<String, TreeAutomaton<String>> binarizationTermsPerInterpretation = new HashMap<String, TreeAutomaton<String>>();
        Map<String, Int2ObjectMap<IntSet>> varPerInterpretation = new HashMap<String, Int2ObjectMap<IntSet>>();
        RuleBinarization ret = new RuleBinarization();
        
        System.err.println("\nBinarizing rule: " + rule.toString(irtg.getAutomaton()));

        for (String interpretation : irtg.getInterpretations().keySet()) {
            String label = irtg.getAutomaton().getSignature().resolveSymbolId(rule.getLabel());            // this is alpha from the paper
            Tree<String> rhs = irtg.getInterpretation(interpretation).getHomomorphism().get(label);        // this is h_i(alpha)
            
            System.err.println("** interpretation " + interpretation + ": alpha=" + label + ", rhs=" + rhs);

            TreeAutomaton<String> binarizationTermsHere = regularSeeds.get(interpretation).binarize(rhs);  // this is G_i
            binarizationTermsPerInterpretation.put(interpretation, binarizationTermsHere);
            
            System.err.println("\nG_i:\n" + binarizationTermsHere);   
            System.err.println("lang(Gi) = " + binarizationTermsHere.language());

            Int2ObjectMap<IntSet> varHere = computeVar(binarizationTermsHere);                             // this is var_i
            varPerInterpretation.put(interpretation, varHere);
            
            System.err.println("\nvars_i:" + varHere);

            TreeAutomaton<IntSet> variableTrees = vartreesForAutomaton(binarizationTermsHere, varHere);    // this is G'_i  (accepts variable trees)
            
            System.err.println("\nG'_i:\n" + variableTrees);
            System.err.println("lang(G'_i) = " + variableTrees.language());

            if (commonVariableTrees == null) {
                commonVariableTrees = variableTrees;
            } else {
                commonVariableTrees = commonVariableTrees.intersect(variableTrees);
            }

            if (commonVariableTrees.isEmpty()) {
                return null;
            }
        }
        
        System.err.println("\nGrammar for common variable trees:\n" + commonVariableTrees);

        Tree<String> commonVariableTree = commonVariableTrees.viterbi();                                   // this is tau, some vartree they all have in common
        ret.xi = commonVariableTree;
        assert commonVariableTree != null;
        
        System.err.println("\nSelected vartree: " + commonVariableTree);

        for (String interpretation : irtg.getInterpretations().keySet()) {
            TreeAutomaton binarizationsForThisVartree = binarizationsForVartree(binarizationTermsPerInterpretation.get(interpretation), commonVariableTree, varPerInterpretation.get(interpretation)); // this is G''_i
            Tree<String> binarization = binarizationsForThisVartree.viterbi();
            ret.binarizationTerms.put(interpretation, binarization);
        }

        return ret;
    }

    /**
     * **********************************************************************************
     *
     * Step (ii) of the algorithm: Compute G'_i from G_i. G'_i accepts language
     * of variable trees of the trees in L(G_i).
     *
     ***********************************************************************************
     */
    // step (ii) of the algorithm: construct G'_i from G_i and vars_i
    // The automata computed by this method look like this:
    // '{2}' -> '2' [1.0]
    // '{1, 0}' -> '0_1'('{0}', '{1}') [1.0]
    // '{1}' -> '1' [1.0]
    // '{1, 2, 0}'! -> '0_1_2'('{1, 0}', '{2}') [1.0]
    // '{1, 2, 0}'! -> '0_1_2'('{0}', '{1, 2}') [1.0]
    // '{0}' -> '0' [1.0]
    // '{1, 2}' -> '1_2'('{1}', '{2}') [1.0]
    static TreeAutomaton<IntSet> vartreesForAutomaton(TreeAutomaton<String> automaton, Int2ObjectMap<IntSet> vars) {
        ConcreteTreeAutomaton<IntSet> ret = new ConcreteTreeAutomaton<IntSet>();

        for (Rule rule : automaton.getRuleSet()) {
            if (rule.getArity() == 0) {
                String label = automaton.getSignature().resolveSymbolId(rule.getLabel());

                if (label.startsWith("?")) {
                    int var = HomomorphismSymbol.getVariableIndex(label);

                    IntSet is = new IntOpenHashSet();
                    is.add(var);

                    Rule newRule = ret.createRule(is, representVarSet(is), new ArrayList<IntSet>());
                    ret.addRule(newRule);
                }
            } else {
                List<IntSet> rhsVarsets = new ArrayList<IntSet>();

                for (int i = 0; i < rule.getArity(); i++) {
                    IntSet varset = vars.get(rule.getChildren()[i]);  // TODO - interpret null as empty IntSet

                    // TODO - here I'm collecting only nonempty varsets; in (iv) below I'm collecting all varsets
                    // -> what I'm doing here is probably correct
                    if (!varset.isEmpty()) {
                        rhsVarsets.add(varset);
                    }
                }

                if (rhsVarsets.size() >= 2) {
                    Collections.sort(rhsVarsets, new IntSetComparator());

                    IntSet parentSet = vars.get(rule.getParent());
                    Rule newRule = ret.createRule(parentSet, representVarSet(parentSet), rhsVarsets);
                    ret.addRule(newRule);

                    if (automaton.getFinalStates().contains(rule.getParent())) {
                        ret.addFinalState(newRule.getParent());
                    }
                }
            }
        }
        return ret;
    }

    // TODO - this could probably be faster
    private static class IntSetComparator implements Comparator<IntSet> {

        public int compare(IntSet o1, IntSet o2) {
            return representVarSet(o1).compareTo(representVarSet(o2));
        }
    }

    private void addEntriesToHomomorphism(Homomorphism hom, Tree<String> xi, Tree<String> binarizationTerm) {
        hom.getTargetSignature().addAllSymbols(binarizationTerm);

        Tree<HomomorphismSymbol> binarizationTermHS = HomomorphismSymbol.treeFromNames(binarizationTerm, hom.getTargetSignature());
        Tree<Tree<HomomorphismSymbol>> decompositionTree = makeMaximalDecomposition(binarizationTermHS);
        Tree<Tree<HomomorphismSymbol>> recombinedTree = merge(decompositionTree);

        Tree<Set<HomomorphismSymbol>> vartree = vartree(recombinedTree, new Function<Tree<HomomorphismSymbol>, HomomorphismSymbol>() {
            public HomomorphismSymbol apply(Tree<HomomorphismSymbol> f) {
                return f.getLabel();
            }
        });


        // this is probably wrong: why should xi be mapped into HomSymbols?? 29.08.13

//        Tree<Set<HomomorphismSymbol>> xiVartree = vartree(xi, new Function<HomomorphismSymbol, HomomorphismSymbol>() {
//            public HomomorphismSymbol apply(HomomorphismSymbol f) {
//                return f;
//            }
//        });

//        constructHomomorphism(xi, xiVartree, recombinedTree, vartree, hom);
    }

    private Tree<Tree<HomomorphismSymbol>> makeMaximalDecomposition(Tree<HomomorphismSymbol> binarizationTerm) {
        final List<Tree<HomomorphismSymbol>> treesForVariables = new ArrayList<Tree<HomomorphismSymbol>>();

        return binarizationTerm.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<Tree<HomomorphismSymbol>>>() {
            @Override
            public Tree<Tree<HomomorphismSymbol>> combine(Tree<HomomorphismSymbol> node, List<Tree<Tree<HomomorphismSymbol>>> childrenValues) {
                if (node.getLabel().isVariable()) {
                    return Tree.create(node);
                } else {
                    Tree<HomomorphismSymbol>[] variables = new Tree[childrenValues.size()];

                    ensureSize(treesForVariables, childrenValues.size());
                    for (int i = 0; i < childrenValues.size(); i++) {
                        variables[i] = treesForVariables.get(i);
                    }

                    Tree<HomomorphismSymbol> newLabel = Tree.create(node.getLabel(), variables);
                    return Tree.create(newLabel, childrenValues);
                }
            }
        });
    }

    private void ensureSize(List<Tree<HomomorphismSymbol>> treesForVariables, int capacity) {
        for (int i = treesForVariables.size(); i < capacity; i++) {
            treesForVariables.add(Tree.create(var(i)));
        }
    }

    private Tree<Tree<HomomorphismSymbol>> merge(Tree<Tree<HomomorphismSymbol>> decompositionTree) {
        return decompositionTree.dfs(new TreeVisitor<Tree<HomomorphismSymbol>, Void, Tree<Tree<HomomorphismSymbol>>>() {
            @Override
            public Tree<Tree<HomomorphismSymbol>> combine(Tree<Tree<HomomorphismSymbol>> node, List<Tree<Tree<HomomorphismSymbol>>> childrenValues) {
                List<Tree<Tree<HomomorphismSymbol>>> remainingChildren = new ArrayList<Tree<Tree<HomomorphismSymbol>>>();
                Tree<HomomorphismSymbol> label = node.getLabel();

                // merge children with 0 or 1 variables into label
                for (int i = 0; i < childrenValues.size(); i++) {
                    Tree<Tree<HomomorphismSymbol>> child = childrenValues.get(i);

                    switch (child.getChildren().size()) {
                        case 0:
                            label = substituteVariable(label, i, child.getLabel(), true);
                            break;

                        case 1:
                            label = substituteVariable(label, i, child.getLabel(), false);
                            remainingChildren.add(child.getChildren().get(0));
                            break;

                        default:
                            remainingChildren.add(child);
                    }
                }

                // TODO - consider case where label now only has one variable left, this
                // requires merging too

                // recombine
                return Tree.create(label, remainingChildren);
            }

            private Tree<HomomorphismSymbol> substituteVariable(Tree<HomomorphismSymbol> label, final int varnumToReplace, Tree<HomomorphismSymbol> replacement, final boolean deleteVariable) {
                final Tree<HomomorphismSymbol> renamedReplacement = renameVariable(replacement, var(0), var(varnumToReplace));

                return label.substitute(new Function<Tree<HomomorphismSymbol>, Tree<HomomorphismSymbol>>() {
                    public Tree<HomomorphismSymbol> apply(Tree<HomomorphismSymbol> t) {
                        HomomorphismSymbol label = t.getLabel();

                        if (label.isVariable()) {
                            int varnum = label.getValue();

                            if (varnum == varnumToReplace) {
                                return renamedReplacement;
                            } else if (deleteVariable && varnum > varnumToReplace) {
                                return Tree.create(var(varnum - 1));
                            }
                        }

                        return null;
                    }
                });
            }

            private Tree<HomomorphismSymbol> renameVariable(Tree<HomomorphismSymbol> tree, final HomomorphismSymbol oldVarname, final HomomorphismSymbol newVarname) {
                return tree.substitute(new Function<Tree<HomomorphismSymbol>, Tree<HomomorphismSymbol>>() {
                    public Tree<HomomorphismSymbol> apply(Tree<HomomorphismSymbol> f) {
                        if (f.getLabel().equals(oldVarname)) {
                            return Tree.create(newVarname);
                        } else {
                            return null;
                        }
                    }
                });
            }
        });
    }

    private <E> Tree<Set<HomomorphismSymbol>> vartree(Tree<E> tree, final Function<E, HomomorphismSymbol> labelFunction) {
        return tree.dfs(new TreeVisitor<E, Void, Tree<Set<HomomorphismSymbol>>>() {
            @Override
            public Tree<Set<HomomorphismSymbol>> combine(Tree<E> node, List<Tree<Set<HomomorphismSymbol>>> childrenValues) {
                Set<HomomorphismSymbol> vars = new HashSet<HomomorphismSymbol>();
                HomomorphismSymbol label = labelFunction.apply(node.getLabel());

                if (label.isVariable()) {
                    vars.add(label);
                } else {
                    for (Tree<Set<HomomorphismSymbol>> child : childrenValues) {
                        Set<HomomorphismSymbol> childVars = child.getLabel();
                        assert disjoint(childVars, vars);
                        vars.addAll(childVars);
                    }
                }

                return Tree.create(vars, childrenValues);
            }
        });
    }

    private static <E> boolean disjoint(Set<E> s1, Set<E> s2) {
        Set<E> x = new HashSet<E>(s1);
        x.retainAll(s2);
        return x.isEmpty();
    }

    private void constructHomomorphism(Tree<HomomorphismSymbol> xi, Tree<Set<HomomorphismSymbol>> xiVartree, Tree<Tree<HomomorphismSymbol>> recombinedTree, Tree<Set<HomomorphismSymbol>> vartree, Homomorphism hom) {
        assert xi.getChildren().size() == recombinedTree.getChildren().size();

        if (xi.getLabel().isVariable()) {
            assert recombinedTree.getLabel().getLabel().equals(xi.getLabel());
        } else {
            hom.add(hom.getSourceSignature().getIdForSymbol(xi.getLabel().toString()), recombinedTree.getLabel()); // TODO - is this right?

            for (int xiChild = 0; xiChild < xi.getChildren().size(); xiChild++) {
                Set<HomomorphismSymbol> xiChildVartree = xiVartree.getChildren().get(xiChild).getLabel();
                boolean foundPartner = false;

                for (int treeChild = 0; treeChild < recombinedTree.getChildren().size(); treeChild++) {
                    if (vartree.getChildren().get(treeChild).getLabel().equals(xiChildVartree)) {
                        foundPartner = true;
                        constructHomomorphism(xi.getChildren().get(xiChild), xiVartree.getChildren().get(xiChild),
                                recombinedTree.getChildren().get(treeChild), vartree.getChildren().get(treeChild), hom);
                    }
                }

                assert foundPartner;
            }
        }
    }

    /**
     * **********************************************************************************
     *
     * Step (iv) of the algorithm: Compute G''_i from G'_i, var_i, and tau.
     * G''_i accepts the binarization trees that are consistent with tau.
     *
     ***********************************************************************************
     */
    // step (iv) of the algorithm: compute G''_i from G_i, var_i, and tau
    static TreeAutomaton<String> binarizationsForVartree(TreeAutomaton<String> binarizations, Tree<String> commonVariableTree, Int2ObjectMap<IntSet> var) {
        ConcreteTreeAutomaton<String> ret = new ConcreteTreeAutomaton<String>();
        Set<String> forksInVartree = collectForks(commonVariableTree);

        for (Rule rule : binarizations.getRuleSet()) {
            List<IntSet> rhsVarsets = new ArrayList<IntSet>();
            int nonemptyVarsets = 0;
            boolean keepRule = false;

            for (int i = 0; i < rule.getArity(); i++) {
                IntSet varset = var.get(rule.getChildren()[i]);
                rhsVarsets.add(varset);

                if (!varset.isEmpty()) {
                    nonemptyVarsets++;
                }
            }

            if (nonemptyVarsets < 2) {
                keepRule = true;
            } else {
                String ruleFork = representVarSets(rhsVarsets);
                keepRule = forksInVartree.contains(ruleFork);
            }

            if (keepRule) {
                Rule newRule = transferRule(rule, binarizations, ret);
                ret.addRule(newRule);

                if (binarizations.getFinalStates().contains(rule.getParent())) {
                    ret.addFinalState(newRule.getParent());
                }
            }
        }

        return ret;
    }

    private static String representVarSet(IntSet vs) {
        int[] vars = vs.toIntArray();
        Arrays.sort(vars);

        StringBuilder buf = new StringBuilder();
        boolean first = true;

        for (int i = 0; i < vars.length; i++) {
            if (first) {
                first = false;
            } else {
                buf.append("_");
            }

            buf.append(vars[i]);
        }

        return buf.toString();
    }

    private static String representVarSets(Collection<IntSet> vss) {
        SortedSet<String> reprs = new TreeSet<String>();

        for (IntSet vs : vss) {
            if (!vs.isEmpty()) {
                reprs.add(representVarSet(vs));
            }
        }

        return StringTools.join(reprs, "+");
    }

    static Set<String> collectForks(Tree<String> vartree) {
        final Set<String> ret = new HashSet<String>();

        vartree.dfs(new TreeVisitor<String, Void, IntSet>() {
            @Override
            public IntSet combine(Tree<String> node, List<IntSet> childrenValues) {
                IntSet here = new IntOpenHashSet();

                if (node.getLabel().startsWith("?")) {
                    int var = HomomorphismSymbol.getVariableIndex(node.getLabel());
                    here.add(var);
                    ret.add(representVarSet(here));
                } else if (node.getChildren().isEmpty()) {
                    ret.add(representVarSet(here));
                } else {
                    ret.add(representVarSets(childrenValues));

                    for (IntSet is : childrenValues) {
                        here.addAll(is);
                    }
                }

                return here;
            }
        });

        return ret;
    }

    private static <E> Rule transferRule(Rule oldRule, TreeAutomaton<E> fromAutomaton, TreeAutomaton<E> toAutomaton) {
        List<E> ruleRhs = new ArrayList<E>();

        for (int i = 0; i < oldRule.getArity(); i++) {
            ruleRhs.add(fromAutomaton.getStateForId(oldRule.getChildren()[i]));
        }

        Rule ret = toAutomaton.createRule(fromAutomaton.getStateForId(oldRule.getParent()), fromAutomaton.getSignature().resolveSymbolId(oldRule.getLabel()), ruleRhs);
        return ret;
    }

    private static class RuleBinarization {
        Tree<String> xi;
        Map<String, Tree<String>> binarizationTerms;

        public RuleBinarization() {
            binarizationTerms = new HashMap<String, Tree<String>>();
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + (this.xi != null ? this.xi.hashCode() : 0);
            hash = 11 * hash + (this.binarizationTerms != null ? this.binarizationTerms.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RuleBinarization other = (RuleBinarization) obj;
            if (this.xi != other.xi && (this.xi == null || !this.xi.equals(other.xi))) {
                return false;
            }
            if (this.binarizationTerms != other.binarizationTerms && (this.binarizationTerms == null || !this.binarizationTerms.equals(other.binarizationTerms))) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "<" + xi + " " + binarizationTerms + ">";
        }
        
        
    }

    private String gensym(String prefix) {
        return prefix + (nextGensym++);
    }

    private HomomorphismSymbol var(int i) {
        return HomomorphismSymbol.createVariable("?" + (i + 1));
    }

    static Int2ObjectMap<IntSet> computeVar(TreeAutomaton auto) {
        Int2ObjectMap<IntSet> ret = new Int2ObjectOpenHashMap<IntSet>(); // ret(state) = set of dominated variables in trees derivable from state

        stateLoop:
        for (Integer state : (List<Integer>) auto.getStatesInBottomUpOrder()) {
            Collection<Integer> labelsForState = auto.getLabelsTopDown(state);

            for (int label : labelsForState) {
                for (Rule rule : (Set<Rule>) auto.getRulesTopDown(label, state)) {
                    String labelString = auto.getSignature().resolveSymbolId(label);
                    IntSet s = new IntOpenHashSet();

                    if (labelString.startsWith("?")) {
                        s.add(HomomorphismSymbol.getVariableIndex(labelString));
                    } else {
                        for (int childState : rule.getChildren()) {
                            s.addAll(ret.get(childState));
                        }
                    }

                    ret.put(state, s);
                    continue stateLoop;
                }
            }
        }

        return ret;
    }
}
