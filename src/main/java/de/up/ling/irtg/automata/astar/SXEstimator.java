/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.util.Pair;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <State>
 */
public class SXEstimator<State> implements Estimator<State, SXInside, SXOutside> {
    private final TreeAutomaton<State> grammar;
    private Int2ObjectMap<Set<Rule>> rhsSymbolToRules;  //< maps a symbol to a set of rules, where it occurs on the rhs
    private IntSet terminalSymbols;                     //< set of all symbols, that are the parent of a 0-ary rule.
    private SXSummarizer summarizer;
    
    public SXEstimator(TreeAutomaton<State> grammar) {
        this.grammar = grammar;
        // TODO assert grammar in CNF
        sortRulesByRHS();
        fetchTerminalSymbols();
        printRhsSymbolToRules();
        summarizer = null;
    }
    
    public void setSummarizer(SXSummarizer summarizer) {
        this.summarizer = summarizer;
    }
    
    @Override
    public SXInside getSummary(int span) {
        if (summarizer != null) {
            return summarizer.summarizeInside(new StringAlgebra.Span(0, span));
        } else return null;
    }
    
    @Override
    public TreeAutomaton<State> getGrammar() {
        return grammar;
    }
    
    @Override
    public Set<Rule> getRulesForRHSState(int state) {
        return rhsSymbolToRules.get(state);
    }
    
    @Override
    public boolean isTerminal(int state) {
        return terminalSymbols.contains(state);
    }
       
    private void sortRulesByRHS() {
        // Calculation of the outside estimate requires access to rules that have 
        // a certain state on their rhs.
        rhsSymbolToRules = new Int2ObjectOpenHashMap<>();
        rhsSymbolToRules.defaultReturnValue(new HashSet<>());
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
        // collect all terminal symbols.
        // The grammar is in CNF, therefore terminals ... TODO
        terminalSymbols = new IntOpenHashSet();
        grammar.getRuleIterable().forEach((Rule r) -> {
            if (r.getArity() == 0) {
                terminalSymbols.add(r.getParent()); // not r.rhs[0]?
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
    public void forEachRuleOutside(SXOutside outsideSummary, int symbol, int arity, int position, BiConsumer<SXOutside, Pair<SXInside, Integer>> todo) {
        // outsideSummary is left child
        // e.g.: for forEachRule((3,2), *, 2, 0, todo), call “todo” on 
        //      (3,1) -> *((3,2), 1); 
        //      (3,0) -> *((3,2), 2); etc.
        for (int i = 1; i <= outsideSummary.getWordsRight(); ++i) {
            todo.accept(
                    // Outside Summary for the parent state, assuming the given outside summary is the LEFT child
                    new SXOutside(outsideSummary.getWordsLeft(), outsideSummary.getWordsRight() - i),
                    // Inside Summary for the state at the second position (= 1), because we assume the given outside summary is the LEFT child
                    new Pair<>(new SXInside(i), 1) 
            );
        }

        // outsideSummary is right child
        // e.g.:for  forEachRule((3, 2), *, 2, 1, todo) calls “todo" on
        //      (2, 2) -> *(1, (3,2)); 
        //      (1, 2) -> *(2, (3,2)); etc.
        for (int i = 1; i <= outsideSummary.getWordsLeft(); ++i) {
            todo.accept(
                    // Outside Summary for the parent state, assuming the given outside summary is the RIGHT child
                    new SXOutside(outsideSummary.getWordsLeft() - i, outsideSummary.getWordsRight()),
                    // Inside Summary for the state at the first position (= 0), because we assume the given outside summary is the RIGHT child
                    new Pair<>(new SXInside(i), 0) 
            );        
        }
    }

    @Override
    public void forEachRuleInside(SXInside insideSummary, BiConsumer<SXInside, SXInside> todo) {
//        System.err.println("forEachRuleInside: running for " + insideSummary);
        for (int split = 1; split < insideSummary.getSpan(); ++split) {
//            System.err.println("forEachRuleInside: current split = " + split);
//            System.err.println("forEachRuleInside: creating " + new SXInside(split));
//            System.err.println("forEachRuleInside: creating " + new SXInside(insideSummary.getSpan() - split));

            todo.accept(new SXInside(split), new SXInside(insideSummary.getSpan() - split));

        }
    }
    
    @Override
    public boolean isOutsideSummaryComplete(SXOutside outsideSummary) {
        return outsideSummary.getWordsLeft() == 0 && outsideSummary.getWordsRight() == 0;
    }

    @Override
    public boolean isInsideSummaryTerminal(SXInside insideSummary) {
        return insideSummary.getSpan() == 1;
    }
    
    private void printRhsSymbolToRules() {
        rhsSymbolToRules.keySet().forEach(key -> {
            rhsSymbolToRules.get(key).forEach(rule -> {
                System.out.println(key + " |->  '" + rule.toString(grammar) + "'");
            });
        });

    }

   
   
}
