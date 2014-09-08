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
import java.util.Arrays;
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
