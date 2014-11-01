/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.util.MutableDouble;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javafx.util.Pair;

/**
 *
 * @author gontrum
 * @param <State>
 * @param <InsideSummary>
 * @param <OutsideSummary>
 */
public class AStarEstimator<State, InsideSummary extends Summary, OutsideSummary extends Summary> {
    private final AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator;
    private final TreeAutomaton<State> grammar;
    private Int2ObjectMap<Set<Rule>> rhsSymbolToRules;  //< maps a symbol to a set of rules, where it occurs on the rhs
    private IntSet terminalSymbols;                     //< set of all symbols, that are the parent of a 0-ary rule.

    // Caches
    private final Object2DoubleMap<OutsideSummary> outsideCache; //< maps an outside summary + a state to a cached estimation
    private final Object2DoubleMap<InsideSummary> insideCache;  //< maps an inside summary + a state to a cached estimation

 
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator, TreeAutomaton<State> grammar) {
        this.estimator = estimator;
        this.grammar = grammar;
        
        this.outsideCache = new Object2DoubleOpenHashMap<>();
        this.insideCache = new Object2DoubleOpenHashMap<>();
        
        sortRulesByRHS();
        fetchTerminalSymbols();
    }
    
    // Datastructure functions
    //   Build datastructures 
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
        terminalSymbols = new IntOpenHashSet();
        grammar.getRuleIterable().forEach((Rule r) -> {
            if (r.getArity() == 0) {
                terminalSymbols.add(r.getParent());
            }
        });
    }
    
    private void saveInInsideCache(InsideSummary key, double value) {
        if (value > getInsideCache(key)) {
            insideCache.put(key, value);
        }
    }
    
    private void saveInOutsideCache(OutsideSummary key, double value) {
        if (value > getOutsideCache(key)) {
            outsideCache.put(key, value);
        }
    }
    
    //   Access datastructures
    
    //     Cache
    private double getInsideCache(InsideSummary key) {
        return (checkInsideCache(key))? insideCache.get(key) : Double.NEGATIVE_INFINITY;
    }
    
    private double getOutsideCache(OutsideSummary key) {
        return (checkOutsideCache(key)) ? outsideCache.get(key) : Double.NEGATIVE_INFINITY;
    }
    
    private boolean checkInsideCache(InsideSummary key) {
        return insideCache.containsKey(key);
    }

    private boolean checkOutsideCache(OutsideSummary key) {
        return outsideCache.containsKey(key);
    }
    
    //     Grammar
    
    private boolean isTerminal(int state) {
        return terminalSymbols.contains(state);
    }
    
    // Return all rules, that have a given symbol on their rhs.
    private Set<Rule> getRulesForRHSSymbol(int symbol) {
        if (rhsSymbolToRules.containsKey(symbol)) {
            return rhsSymbolToRules.get(symbol);
        } else {
            return new HashSet<>();
        }
    }
    
    private void printRhsSymbolToRules() {
        rhsSymbolToRules.keySet().forEach(key -> {
            rhsSymbolToRules.get(key).forEach(rule -> {
                System.out.println(key + " |->  '" + rule.toString(grammar) + "'");
            });
        });

    }

    
    // estimate Outside & Inside

    public double estimateOutside(int state, OutsideSummary outsideSummary) {
        System.err.println("\nestimateOutside: New run for state '" + grammar.getStateForId(state) + "' and outsideSummary "+ outsideSummary);
        
        // check caches first
        if (checkOutsideCache(outsideSummary)) {
            System.err.println("estimateOutside: Returning from cache: " + getOutsideCache(outsideSummary));
            return getOutsideCache(outsideSummary);
        } else {
            // set marker to identify circles in the automaton
            saveInOutsideCache(outsideSummary, Double.NEGATIVE_INFINITY);
            
            // start calculation
            // base case for the recursion: summary is complete 
            if (estimator.isOutsideSummaryComplete(outsideSummary)) {
                System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): outsideSummary is complete, returning " + (grammar.getFinalStates().contains(state) ? 0 : Double.NEGATIVE_INFINITY));
                // if the state is the startsymbol of the grammar, return 0
                return grammar.getFinalStates().contains(state) ? 0 : Double.NEGATIVE_INFINITY;
            }

            MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
            getRulesForRHSSymbol(state).stream().forEach((r) -> {
                System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Current rule for " + outsideSummary + " with state " + grammar.getStateForId(state) + " : " + r.toString(grammar));
                int position = 0;
                while (state != r.getChildren()[position]) {
                    ++position;
                } // TODO multiple positions?
                System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): pos: " + position);
                // Getting Outside Summaries and a pairs of an Inside Summary and an Integer that shows the position of the IS
                estimator.forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, (OutsideSummary os, InsideSummary[] insideSummaries) -> {
//                    System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): forEachRuleOutside for rule " + r.toString(grammar)
//                            + "\n  Outside Summary: " + os
//                            + "\n  Inside Summary: " + iSPositionPair.getKey() + " on position " + iSPositionPair.getValue());

                    double currentEstimate = Math.log(r.getWeight()); // P(rule) ...
                    System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): currentEstimate P(Rule) -> " + currentEstimate);

                    for (int i = 0; i < insideSummaries.length; i++) {
                        if (insideSummaries[i] != null) {
                            System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Running inside estimation for state " + grammar.getStateForId(r.getChildren()[i]) + " and " + insideSummaries[i] + " on position " + i);
                            currentEstimate += estimateInside(r.getChildren()[i], insideSummaries[i]);
                            System.err.println("\nestimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): currentEstimate -> " + currentEstimate);
                        }
                        
                    }
                   
                    System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Running outside estimation for state " + grammar.getStateForId(r.getParent()) + " and " + os);
                    currentEstimate += estimateOutside(r.getParent(), os); // ... * estimateOutside(A,s) ...

                    score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights
                });
            });

            System.err.println("estimateOutsideestimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): returning=" + score.getValue() + "\n");
            
            // store value in cache
            saveInOutsideCache(outsideSummary, score.getValue());
            
            return score.getValue();
        }
    }
    
    public double estimateInside(int state, InsideSummary insideSummary) {
        System.err.println("\nestimateInside: New run for state=" + grammar.getStateForId(state) + " is="+insideSummary);
        
        if (checkInsideCache(insideSummary)) {
            // found in cache
            System.err.println("nestimateInside: Returning from cache: " + getInsideCache(insideSummary));
            return getInsideCache(insideSummary);
        } else {
            // set marker to avoid circles
            saveInInsideCache(insideSummary, Double.NEGATIVE_INFINITY);
            
            
            // if IS == 0: 
            //      if state \in terminal -> 0
            //      else -> neg infinity
            if (estimator.isInsideSummaryTerminal(insideSummary)) {
                if (isTerminal(state)) {
                    System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): IS is terminal and state is terminal. Returning 0.");
                    // maximize over rules and return the best one.
                    double ret = 0;
                    for (Rule r : grammar.getRulesTopDown(state)) { 
                        if (r.getWeight() > ret) ret = r.getWeight();
                    }
                    return Math.log(ret);
                }
            }

            MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);

        // for rule state -> x y:
            //      for split in 1 -> span-1:
            //          inside(x, split)
            //          inside(y, span - split)
            //          P(rule)
            for (Rule r : grammar.getRulesTopDown(state)) {
                System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): Current rule: " + r.toString(grammar));
                if (r.getArity() == 2) {
                    estimator.forEachRuleInside(insideSummary, r.getArity(),
                            newInsideSummaries -> {
                                System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): forEachRuleInside for rule " + r.toString(grammar)
                                        + "\n  new InsideSummaries: " + Arrays.toString(newInsideSummaries));

                                double currentEstimate = Math.log(r.getWeight());
                                System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): current estimate P(rule): " + currentEstimate);
                                
                                for (int i = 0; i < newInsideSummaries.length; i++) {
                                    System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): Starting estimateInside for " + grammar.getStateForId(r.getChildren()[0]) + " and " + newInsideSummaries[i]);
                                    currentEstimate += estimateInside(r.getChildren()[i], newInsideSummaries[i]);
                                    System.err.println("\nestimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): current estimate: " + currentEstimate);
                                    
                                }
            
                                score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights 
                            });
                }

            }

            System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): returning " + score.getValue());
            
            saveInInsideCache(insideSummary, score.getValue());
            
            return score.getValue();
        }
       
    }
    
    
    
    
    
    
    
    // Main method for testing purposes only 
    public static void main(String[] args) throws ParseException, IOException, ParserException {
        String scfgIRTG  = "interpretation english: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "interpretation german: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "\n"
            + "\n"
            + "S! -> r1(NP,VP) [1.0]\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "\n"
            + "NP -> r2(Det,N) [0.8]\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "N -> r3(N,PP) [0.4]\n" 
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "VP -> r4(V,NP) [0.7]\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "VP -> r5(VP,PP) [0.3]\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "PP -> r6(P,NP) [1.0]\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "NP -> r7 [0.2]\n"
            + "  [english] john\n"
            + "  [german] hans\n"
            + "\n"
            + "V -> r8 [1.0]\n"
            + "  [english] watches\n"
            + "  [german] betrachtet\n"
            + "\n"
            + "Det -> r9 [0.4] \n"
            + "  [english] the\n"
            + "  [german] die\n"
            + "\n"
            + "Det -> r9b [0.6]\n"
            + "  [english] the\n"
            + "  [german] dem\n"
            + "\n"
            + "N -> r10 [0.3]\n"
            + "  [english] woman\n"
            + "  [german] frau\n"
            + "\n"
            + "N -> r11 [0.3]\n"
            + "  [english] telescope\n"
            + "  [german] fernrohr\n"
            + "\n"
            + "P -> r12 [1.0]\n"
            + "  [english] with\n"
            + "  [german] mit\n";
        
        String testIRTG = "interpretation i: de.up.ling.irtg.algebra.StringAlgebra\n" +
"\n" +
"A! -> r1(B, C)\n" +
"    [i] *(?1, ?2)\n" +
"\n" +
"B -> r2\n" +
"    [i] b\n" +
"\n" +
"C -> r3\n" +
"    [i] c\n" +
"\n" +
"C -> r4(D, E)\n" +
"    [i] *(?1, ?2)\n" +
"\n" +
"D -> r5 \n" +
"    [i] d\n" +
"\n" +
"E -> r6\n" +
"    [i] e";
        
//        SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
//        
//        estimator.forEachRuleOutside(new SXOutside(1,1), 0, 2, 0, (SXOutside os, SXInside[] iss) -> {
//            System.err.println(os);
//            for (SXInside is : iss) {
//                System.err.println(is);
//            }
//            System.err.println("");
//        });
        
        
        
////
//        SXAlgebraStructureSummary.generate(0, 2, new int[3], tup -> {
//            System.err.println(Arrays.toString(tup));
//        });
//        
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(scfgIRTG);
//        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(testIRTG);

        String input = "der mann beobachtet die frau mit dem fernrohr";        
//        String input = "der mann beobachtet die frau mit dem fernrohr";

//        String input = "d e";

        System.err.println(Arrays.asList(input.split(" ")));
        
        
        SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
        SXSummarizer summarizer = new SXSummarizer(Arrays.asList(input.split(" ")));

        AStarEstimator<String, SXInside, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
        
//        estimator.forEachRuleInside(new SXInside(4), 2, tup -> {
//            System.err.println(Arrays.toString(tup));
//        });
//        
//        estimator.forEachRuleOutside(new SXOutside(4,3), 0, 3, 2, (SXOutside os, SXInside[] iss) -> {
//            System.err.println(os + " -> " + Arrays.toString(iss));
//
//            System.err.println("");
//        });
////        
       

        int state = irtg.getAutomaton().getIdForState("VP");
//        int state = irtg.getAutomaton().getIdForState("C");


        SXOutside os = summarizer.summarizeOutside(new StringAlgebra.Span(2, 6));
        SXInside is = summarizer.summarizeInside(new StringAlgebra.Span(0, input.split(" ").length));

//        System.err.println("Calculating InsideSummary for the state '" + irtg.getAutomaton().getStateForId(state) + "' over the span " + is);
        System.err.println("Calculating OutsideSummary for the state '" + irtg.getAutomaton().getStateForId(state) + "' over the span " + os);

//            System.err.println(astar.estimateOutside(finalState, is));
        System.err.println("\n\nRestult: " + astar.estimateOutside(state, os));

        
    }
}
