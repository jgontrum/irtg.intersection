/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.util.MutableDouble;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <State>
 */
public class SXEstimator<State> implements Estimator<State, SXInside, SXOutside> {
    private final TreeAutomaton<State> grammar;
    private Int2ObjectMap<Set<Rule>> rhsSymbolToRules;  //< maps a symbol to a set of rules, where it occurs on the rhs
    private IntSet terminalSymbols;                     //< set of all symbols, that are the parent of a 0-ary rule.
    
    public SXEstimator(TreeAutomaton<State> grammar) {
        this.grammar = grammar;
        // TODO assert grammar in CNF
        sortRulesByRHS();
        fetchTerminalSymbols();
    }
    
    private void sortRulesByRHS() {
        // Calculation of the outside estimate requires access to rules that have 
        // a certain state on their rhs.
        rhsSymbolToRules = new Int2ObjectOpenHashMap<>();
        rhsSymbolToRules.defaultReturnValue(new HashSet<Rule>());
        for (Rule r : grammar.getRuleIterable()) {
            for (int symbol : r.getChildren()) {
                if (!rhsSymbolToRules.containsKey(symbol)) {
                    Set<Rule> insert = new HashSet<>();
                    insert.add(r);
                    rhsSymbolToRules.put(symbol, insert);
                } else {
                    rhsSymbolToRules.get(symbol).add(r);
                }
            }
        }
    }

    private void fetchTerminalSymbols() {
        terminalSymbols = new IntOpenHashSet();
        grammar.getRuleIterable().forEach((Rule r) -> {
            if (r.getArity() == 0) {
                terminalSymbols.add(r.getParent());
            }
        });
    }

    private Set<Rule> getRulesForRHSSymbol(int symbol) {
        if (rhsSymbolToRules.containsKey(symbol)) {
            return rhsSymbolToRules.get(symbol);
        } else {
            return new HashSet<>();
        }
    }

    @Override
    public double estimateOutside(int state, SXOutside outsideSummary) {  
        // base case for the recursion: summary is complete 
        if (isOutsideSummaryComplete(outsideSummary)) {
            // if the state is the startsymbol of the grammar, return 0
            return grammar.getFinalStates().contains(state)? 0 : Double.NEGATIVE_INFINITY;
        }
        MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
        for (Rule r : rhsSymbolToRules.get(state)) {
            int position = 0;
            for (; state == r.getChildren()[position]; position++);
            forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, // forEachRuleOutside(outsideSummary, f, n, i):
                    rule -> {
                        assert rule.size() == r.getArity();
                        double currentEstimate = Math.log(r.getWeight()); // P(rule) ...

                        
                        assert rule.get(0).getClass() == outsideSummary.getClass();

                        currentEstimate += estimateOutside(r.getParent(), (SXOutside) rule.get(0)); // ... * estimateOutside(A,s) ...
                        for (int i = 1; i < rule.size(); i++) {
                            if (rule.get(i).getClass() != outsideSummary.getClass()) { // Calculate inside estimates only
                                currentEstimate += estimateInside(r.getChildren()[i], (SXInside) rule.get(i)); // ... * estimateInside(Bi,si) ... 
                            }
                        }
                        score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights
                    });
        }

        return score.getValue();
    }
    
    
    @Override
    public double estimateInside(int state, SXInside insideSummary) {
        // if IS == 0: 
        //      if state \in terminal -> 0
        //      else -> neg infinity
        if (insideSummary.getSpan() == 0) {
            return (terminalSymbols.contains(state) ? 0 : Double.NEGATIVE_INFINITY);
        }

        MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);

        // for rule state -> x y:
        //      for split in 1 -> span-1:
        //          inside(x, split)
        //          inside(y, span - split)
        //          P(rule)
        for (Rule r : grammar.getRulesTopDown(state)) {
            forEachRuleInside(insideSummary,
                    newInsideList -> {
                        assert newInsideList.size() == r.getArity();
                        double currentEstimate = Math.log(r.getWeight());

                        for (int i = 0; i < newInsideList.size(); i++) {
                            currentEstimate += estimateInside(r.getChildren()[i], newInsideList.get(i));
                        }

                        score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights 
                    });
        }

        return score.getValue();
    }
    
    @Override
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, Consumer<List<Object>> todo) {
        // outsideSummary is left child
        // e.g.: for forEachRule((3,2), *, 2, 0, todo), call “todo” on 
        //      (3,1) -> *((3,2), 1); 
        //      (3,0) -> *((3,2), 2); etc.

        for (int i = 1; i <= outsideSummary.getWordsRight(); ++i) {
            List<Object> ret = new ArrayList<>();
            ret.add(new SXOutside(outsideSummary.getWordsLeft(), outsideSummary.getWordsRight() - i));
            ret.add(symbol);

            ret.add(outsideSummary); // do I have to add this to the list? It has to be skipped in estimateOutside anyway?
            ret.add(new SXInside(i)); // i = length = inside?
            todo.accept(ret);
        }

        // outsideSummary is right child
        // e.g.:for  forEachRule((3, 2), *, 2, 1, todo) calls “todo" on
        //      (2, 2) -> *(1, (3,2)); 
        //      (1, 2) -> *(2, (3,2)); etc.
        for (int i = 1; i <= outsideSummary.getWordsLeft(); ++i) {
            List<Object> ret = new ArrayList<>();
            ret.add(new SXOutside(outsideSummary.getWordsLeft() - i, outsideSummary.getWordsRight()));
            ret.add(symbol);
            ret.add(new SXInside(i)); // i = length = inside?
            ret.add(outsideSummary);
            todo.accept(ret);
        }
    }

    @Override
    public void forEachRuleInside(SXInside insideSummary, Consumer<List<SXInside>> todo) {
        List<SXInside> ret = new ArrayList<>();
        for (int split = 1; split < insideSummary.getSpan(); ++split) {
            ret.add(new SXInside(split));
            ret.add(new SXInside(insideSummary.getSpan() - split));
            todo.accept(ret);
        }
    }
    
    @Override
    public boolean isOutsideSummaryComplete(SXOutside outsideSummary) {
        return outsideSummary.getWordsLeft() == 0 && outsideSummary.getWordsRight() == 0;
    }

   
   
}
