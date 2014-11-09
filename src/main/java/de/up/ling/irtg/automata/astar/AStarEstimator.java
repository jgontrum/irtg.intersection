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
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.codec.PcfgIrtgInputCodec;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
    
    private final boolean VERBOSE = false;
    private final boolean DEBUG = true;

    // Caches
    private List<Object2DoubleMap<OutsideSummary>> outsideCaches; 
    private List<Object2DoubleMap<InsideSummary>> insideCaches;  

 
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator, TreeAutomaton<State> grammar) {
        this.estimator = estimator;
        this.grammar = grammar;
        
        outsideCaches = new ArrayList<>();
        insideCaches = new ArrayList<>();

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
        
//        for (int sym : terminalSymbols) {
//            System.err.println(grammar.getStateForId(sym));
//        }
        }
    
    private void saveInInsideCache(InsideSummary key, double value, int state) {
        // Check if state is new
        if (state >= insideCaches.size()) {
            // Create entries for all states smaller equal to the current one
            for (int i = insideCaches.size(); i <= state; ++i) {
                insideCaches.add(new Object2DoubleOpenHashMap<>());
                insideCaches.get(i).defaultReturnValue(Double.NaN); // Return NaN by default
            }
        }
        Object2DoubleMap<InsideSummary> currentMap = insideCaches.get(state);
        
        Double oldValue = currentMap.get(key);
        if( oldValue == null || value > oldValue ) {
            currentMap.put(key, value);
        }
    }
    
    private void saveInOutsideCache(OutsideSummary key, double value, int state) {
        // Check if state is new
        if (state >= outsideCaches.size()) {
            // Create entries for all states smaller equal to the current one
            for (int i = outsideCaches.size(); i <= state; ++i) {
                outsideCaches.add(new Object2DoubleOpenHashMap<>());
                outsideCaches.get(i).defaultReturnValue(Double.NaN); // Return NaN by default
            }
        }
        Object2DoubleMap<OutsideSummary> currentMap = outsideCaches.get(state);
        
        Double oldValue = currentMap.get(key);
        
        if( oldValue == null|| value > oldValue ) {
            currentMap.put(key, value);
        }
    }
    
    //   Access datastructures
    
    //     Cache
    private double getInsideCache(InsideSummary key, int state) {
        return (checkInsideCache(key,state))? insideCaches.get(state).get(key) : Double.NEGATIVE_INFINITY;
    }
    
    private double getOutsideCache(OutsideSummary key, int state) {
        return (checkOutsideCache(key, state)) ? outsideCaches.get(state).get(key) : Double.NEGATIVE_INFINITY;
    }
    
    private boolean checkInsideCache(InsideSummary key, int state) {
        if (state >= insideCaches.size()) return false;
        return insideCaches.get(state).containsKey(key);
    }

    private boolean checkOutsideCache(OutsideSummary key, int state) {
        if (state >= outsideCaches.size()) return false;
        return outsideCaches.get(state).containsKey(key);
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
        if (VERBOSE) System.err.println("\nestimateOutside: New run for state '" + grammar.getStateForId(state) + "' and outsideSummary "+ outsideSummary);
        
        // check caches first
        if (checkOutsideCache(outsideSummary, state)) {
            if (VERBOSE) System.err.println("estimateOutside: Returning from cache: " + getOutsideCache(outsideSummary, state));
            return getOutsideCache(outsideSummary, state);
        } else {
            // set marker to identify circles in the automaton
            saveInOutsideCache(outsideSummary, Double.NEGATIVE_INFINITY, state);
            
            // start calculation
            // base case for the recursion: summary is complete 
            if (estimator.isOutsideSummaryComplete(outsideSummary)) {
                if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): outsideSummary is complete, returning " + (grammar.getFinalStates().contains(state) ? 0 : Double.NEGATIVE_INFINITY));
                // if the state is the startsymbol of the grammar, return 0
                return grammar.getFinalStates().contains(state) ? 0 : Double.NEGATIVE_INFINITY;
            }

            MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
            getRulesForRHSSymbol(state).stream().forEach((r) -> {
                if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Current rule for " + outsideSummary + " with state " + grammar.getStateForId(state) + " : " + r.toString(grammar));
                
                // Iterate over all positions in the rhs where the current state matches the state at the position on the rhs
                for (int position = 0; position < r.getArity(); ++position) {
                    if (state == r.getChildren()[position]) {
                        if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): pos: " + position);
                        // Getting Outside Summaries and a pairs of an Inside Summary and an Integer that shows the position of the IS
                        estimator.forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, (OutsideSummary os, InsideSummary[] insideSummaries) -> {
//                      if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): forEachRuleOutside for rule " + r.toString(grammar)
//                            + "\n  Outside Summary: " + os
//                            + "\n  Inside Summary: " + iSPositionPair.getKey() + " on position " + iSPositionPair.getValue());

                            double currentEstimate = Math.log(r.getWeight()); // P(rule) ...
                            if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): currentEstimate P(Rule) -> " + currentEstimate);

                            for (int i = 0; i < insideSummaries.length; i++) {
                                if (insideSummaries[i] != null) {
                                    if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Running inside estimation for state " + grammar.getStateForId(r.getChildren()[i]) + " and " + insideSummaries[i] + " on position " + i);
                                    currentEstimate += estimateInside(r.getChildren()[i], insideSummaries[i]);
                                    if (VERBOSE) System.err.println("\nestimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): currentEstimate -> " + currentEstimate);
                                }

                            }

                            if (VERBOSE) System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): Running outside estimation for state " + grammar.getStateForId(r.getParent()) + " and " + os);
                            currentEstimate += estimateOutside(r.getParent(), os); // ... * estimateOutside(A,s) ...

                            score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights
                        });
                    }
                }
             
            });

            if (VERBOSE) System.err.println("estimateOutsideestimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): returning=" + score.getValue() + "\n");
            
            // store value in cache
            saveInOutsideCache(outsideSummary, score.getValue(), state);
            
            return score.getValue();
        }
    }
    
    public double estimateInside(int state, InsideSummary insideSummary) {
        if (VERBOSE) System.err.println("\nestimateInside: New run for state=" + grammar.getStateForId(state) + " is="+insideSummary);
        
        // TODO - separate caches for different states
        // TODO - do the same for outside
        
        if (checkInsideCache(insideSummary, state)) {  // TODO - low-level zugriffe auf hashmap
            // found in cache
            if (VERBOSE) System.err.println("nestimateInside: Returning from cache: " + getInsideCache(insideSummary, state));
            return getInsideCache(insideSummary, state);
        } else {
            // set marker to avoid circles
            saveInInsideCache(insideSummary, Double.NEGATIVE_INFINITY, state);
            
//            System.err.println("doing work for: " + insideSummary + ", state " + state);
            
            // if IS == 0: 
            //      if state \in terminal -> 0
            //      else -> neg infinity
            if (estimator.isInsideSummaryTerminal(insideSummary)) {
                if (isTerminal(state)) {
                    // maximize over rules and return the best one.
                    double ret = 0;
                    for (Rule r : grammar.getRulesTopDown(state)) { 
                        if (r.getWeight() > ret) ret = r.getWeight();
                    }
                    if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): IS is terminal and state is terminal. Returning " + Math.log(ret) + ".");
                    
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
                if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): Current rule: " + r.toString(grammar));
                estimator.forEachRuleInside(insideSummary, r.getArity(),
                        newInsideSummaries -> {
                            if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): forEachRuleInside for rule " + r.toString(grammar)
                                    + "\n  new InsideSummaries: " + Arrays.toString(newInsideSummaries));

                            double currentEstimate = Math.log(r.getWeight());
                            if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): current estimate P(rule): " + currentEstimate);

                            for (int i = 0; i < newInsideSummaries.length; i++) {
                                if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): Starting estimateInside for " + grammar.getStateForId(r.getChildren()[0]) + " and " + newInsideSummaries[i]);
                                currentEstimate += estimateInside(r.getChildren()[i], newInsideSummaries[i]);
                                if (VERBOSE) System.err.println("\nestimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): current estimate: " + currentEstimate);
                                
                            }

                            score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); // maximize over weights 
                        });
                

            }

            if (VERBOSE) System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): returning " + score.getValue());
            
            saveInInsideCache(insideSummary, score.getValue(), state);
            
            return score.getValue();
        }
       
    }
    
    
    // Main method for testing purposes only 
    public static void main(String[] args) throws ParseException, IOException, ParserException {
        /*
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
        
        String testIRTG = "interpretation i: de.up.ling.irtg.algebra.StringAlgebra\n"
                + "\n"
                + "A! -> r1(B, C)\n"
                + "    [i] *(?1, ?2)\n"
                + "\n"
                + "B -> r2\n"
                + "    [i] b\n"
                + "\n"
                + "C -> r3\n"
                + "    [i] c\n"
                + "\n"
                + "C -> r4(D, E)\n"
                + "    [i] *(?1, ?2)\n"
                + "\n"
                + "D -> r5 \n"
                + "    [i] d\n"
                + "\n"
                + "E -> r6\n"
                + "    [i] e";

        
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(scfgIRTG);

        String input = "der mann beobachtet die frau mit dem fernrohr";
//        String input = "d e";
        System.err.println(Arrays.asList(input.split(" ")));

        SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
        SXSummarizer summarizer = new SXSummarizer(Arrays.asList(input.split(" ")));

        AStarEstimator<String, SXInside, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());

        
//        estimator.forEachRuleInside(new SXInside(10), 2, a -> {
//            Arrays.toString(a);
//        });
        
        int state = irtg.getAutomaton().getIdForState("NP");
//        int state = irtg.getAutomaton().getIdForState("C");

//        SXOutside os = summarizer.summarizeOutside(new StringAlgebra.Span(1, 2));
        SXInside is = summarizer.summarizeInside(new StringAlgebra.Span(0, 2));

        System.err.println("Calculating InsideSummary for the state '" + irtg.getAutomaton().getStateForId(state) + "' over the span " + is);
//        System.err.println("Calculating OutsideSummary for the state '" + irtg.getAutomaton().getStateForId(state) + "' over the span " + os);

            System.err.println(astar.estimateInside(state, is));
//        System.err.println("\n\nRestult: " + astar.estimateOutside(state, os));

        */
        
        if (args.length == 2) {
            String pcfgFilename = args[0];
            String sentencesFilename = args[1];
            
            // Create the IRTG from file
            System.err.println("irtgFilename: " + pcfgFilename);
            InputCodec<InterpretedTreeAutomaton> codec = new IrtgInputCodec();
            InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(pcfgFilename)));
            
            // Create AlgebraStructureSummarizer
            SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
            
            // Initialize the AStarEstimator
            AStarEstimator<String, SXInside, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
            
            // Setting up the instream for the corpus...
            System.err.println("sentencesFilename: " + sentencesFilename);
            FileInputStream instream = new FileInputStream(new File(sentencesFilename));
            DataInputStream in = new DataInputStream(instream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String sentence;

            // ... now iterate over the sentences!
            while ((sentence = br.readLine()) != null) {
                String[] sentence_tok = sentence.split(" ");
                
                System.err.println("Current sentence: " + Arrays.toString(sentence_tok));
                
                // Creating the sentence related summarizer!
                SXSummarizer summarizer = new SXSummarizer(Arrays.asList(sentence_tok));
                
                
                // As a test, calculate Outside Summaries for NNP
//                SXOutside os = summarizer.summarizeOutside(new StringAlgebra.Span(1, sentence_tok.length - 2));
//                int stateos = irtg.getAutomaton().getIdForState("NNP");
                // Run the algorithm:
//                System.err.println("Result for " + irtg.getAutomaton().getStateForId(stateos) + " and outside summary " + os + ":\n  " + astar.estimateOutside(stateos, os));
                
                SXInside is = summarizer.summarizeInside(new StringAlgebra.Span(0, sentence_tok.length));
                int state = irtg.getAutomaton().getIdForState("S");
                // Run the algorithm:
//                System.err.println("Result for " + irtg.getAutomaton().getStateForId(state) + " and inside summary " + is + ":\n  " + astar.estimateInside(state, is));

            }
        } else {
            System.err.println("First argument: Path to IRTG. Second argument: Path to sentences");
            System.err.println("--> " + Arrays.toString(args));
            System.err.println("Len:" + args.length);

        }
        

        
    }
}
