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
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author gontrum
 * @param <State>
 * @param <InsideSummary>
 * @param <OutsideSummary>
 */
public class AStarEstimator<State, InsideSummary, OutsideSummary> {
    private final AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator;
    private final TreeAutomaton<State> grammar;
    private Int2ObjectMap<Set<Rule>> rhsSymbolToRules;  //< maps a symbol to a set of rules, where it occurs on the rhs
    private IntSet nullarySymbols;                      //< set of all symbols, that are the parent of a 0-ary rule.
    
    private final boolean DEBUG = false;

    // Caches
    private List<Object2DoubleMap<OutsideSummary>> outsideCaches; 
    private List<Object2DoubleMap<InsideSummary>> insideCaches;  

 
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator, TreeAutomaton<State> grammar) {
        this.estimator = estimator;
        this.grammar = grammar;
        
        outsideCaches = new ArrayList<>();
        insideCaches = new ArrayList<>();

        sortRulesByRHS();
        collectNullarySymbols();
        initialiseCaches();
    }
    
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> estimator, TreeAutomaton<State> grammar, String cacheFile) {
        this.estimator = estimator;
        this.grammar = grammar;
        
        sortRulesByRHS();
        collectNullarySymbols();
        
        // Read in serialized caches
        try {
            FileInputStream fileIn = new FileInputStream(cacheFile);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            insideCaches = (List<Object2DoubleMap<InsideSummary>>) in.readObject();
            outsideCaches = (List<Object2DoubleMap<OutsideSummary>>) in.readObject();
            in.close();
            fileIn.close();
        } catch (Exception i) {
            System.err.println("Could not read cache from '" + cacheFile + "'");
            Logger.getLogger(AStarEstimator.class.getName()).log(Level.SEVERE, null, i);
            
            outsideCaches = new ArrayList<>();
            insideCaches = new ArrayList<>();
            initialiseCaches();
        } 
    }
    
    /**
     * Estimates the outside probability for a summary and its state.
     * @param state
     * @param outsideSummary
     * @return 
     */
    public double estimateOutside(int state, OutsideSummary outsideSummary) {
        if (DEBUG) {
            System.err.println("\nestimateOutside: New run for state '" + grammar.getStateForId(state) + "' and outsideSummary " + outsideSummary);
        }
        
        Object2DoubleMap<OutsideSummary> stateCache = outsideCaches.get(state);
        double currentCachedValue = stateCache.getOrDefault(outsideSummary, Double.NaN);
        
        // Check cache first
        if (!Double.isNaN(currentCachedValue)) {
            if (DEBUG) {
                System.err.println("estimateOutside: Returning from cache: " + currentCachedValue);
            }
            return currentCachedValue;
        } else {
            // Set marker to identify circles in the automaton
            stateCache.put(outsideSummary, Double.NEGATIVE_INFINITY);
            
            // Base case: summary is complete 
            if (estimator.isOutsideSummaryComplete(outsideSummary)) {
                // if the state is the startsymbol of the grammar, return 0
                double ret = grammar.getFinalStates().contains(state) ? 0 : Double.NEGATIVE_INFINITY;
                
                if (DEBUG) {
                    System.err.println("estimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): outsideSummary is complete, returning " + ret);
                }
                
                stateCache.put(outsideSummary, ret);
                return ret;
            }

            // Recursive case:
            // Estimate the outside value by iterating over all rules where OS 
            // appeares as child and over all positions that OS can have.
            // Calculate the outside estimate and maximize it.
            MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
            getRulesForRHSSymbol(state).stream().forEach((r) -> {
                // Iterate over all positions in the rhs where the current state matches the state at the position on the rhs
                for (int position = 0; position < r.getArity(); ++position) {
                    if (state == r.getChildren()[position]) {
                        // Getting Outside Summaries and a pairs of an Inside Summary and an Integer that shows the position of the IS
                        estimator.forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, (OutsideSummary os, InsideSummary[] insideSummaries) -> {
                            
                            double currentEstimate = Math.log(r.getWeight()); 
                            
                            // Calculate the inside summary for all other child states.
                            for (int i = 0; i < insideSummaries.length; i++) {
                                if (insideSummaries[i] != null) { // insideSummaries is null on the position of OS
                                    currentEstimate += estimateInside(r.getChildren()[i], insideSummaries[i]);
                                }
                            }

                            // Now calculate the outside for the parent state
                            currentEstimate += estimateOutside(r.getParent(), os);
             
                            // Maximize
                            score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); 
                        });
                    }
                }
             
            });

            
            // Store value in cache
            if (DEBUG) {
                System.err.println("estimateOutsideestimateOutside(" + grammar.getStateForId(state) + "," + outsideSummary + "): returning=" + score.getValue() + "\n");
            }
            stateCache.put(outsideSummary, score.getValue());
            
            return score.getValue();
        }
    }
    
    /**
     * Estimate the inside probability of a summary and its state.
     * @param state
     * @param insideSummary
     * @return
     */
    public double estimateInside(int state, InsideSummary insideSummary) {
        if (DEBUG) System.err.println("\nestimateInside: New run for state=" + grammar.getStateForId(state) + " is="+insideSummary);
        
        Object2DoubleMap<InsideSummary> stateCache = insideCaches.get(state);
        double currentCachedValue = stateCache.getOrDefault(insideSummary, Double.NaN);
        
        if (!Double.isNaN(currentCachedValue)) {
            // found in cache
            if (DEBUG) {
                System.err.println("nestimateInside: Returning from cache: " + currentCachedValue);
            }
            
            return currentCachedValue;
        } else {
            // Set marker to avoid circles
            stateCache.put(insideSummary, Double.NEGATIVE_INFINITY);
                        
            // Base case: 
            // If the current inside summary is terminal, return the 
            // weight of the best rule with the state as parent state.
            if (estimator.isInsideSummaryTerminal(insideSummary)) {
                if (isNullarySymbol(state)) {
                    // maximize over rules and return the best one.
                    double ret = 0;
                    for (Rule r : grammar.getRulesTopDown(state)) { 
                        if (r.getWeight() > ret) ret = r.getWeight();
                    }
                    
                    if (DEBUG) {
                        System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): IS is terminal and state is terminal. Returning " + Math.log(ret) + ".");
                    }
                    
                    ret = Math.log(ret);
                    stateCache.put(insideSummary, ret);
                    return ret;
                }
            }
            
            // Recursive case: 
            // Iterate over all rules that have the current state as parent 
            // and maximize over the inside estimate of the child states
            // of the rules.
            MutableDouble score = new MutableDouble(Double.NEGATIVE_INFINITY);
            for (Rule r : grammar.getRulesTopDown(state)) {
                estimator.forEachRuleInside(insideSummary, r.getArity(),
                        newInsideSummaries -> {
                        
                            double currentEstimate = Math.log(r.getWeight());

                            // Get all inside estimates of the children
                            for (int i = 0; i < newInsideSummaries.length; i++) {
                                currentEstimate += estimateInside(r.getChildren()[i], newInsideSummaries[i]);
                            }
                            
                            // Maximize 
                            score.setValue((currentEstimate > score.getValue()) ? currentEstimate : score.getValue()); 
                        });
            }
            
            // Save the value on the cache and return it.
            if (DEBUG) {
                System.err.println("estimateInside(" + grammar.getStateForId(state) + "," + insideSummary + "): returning " + score.getValue());
            }
            stateCache.put(insideSummary, score.getValue());
            return score.getValue();
        }
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

    ////////////////////////////////////////////////////////////
    /////////////// Private helper functions ///////////////////
    ////////////////////////////////////////////////////////////
    private void collectNullarySymbols() {
        // collect all terminal symbols.
        nullarySymbols = new IntOpenHashSet();
        grammar.getRuleIterable().forEach((Rule r) -> {
            if (r.getArity() == 0) {
                nullarySymbols.add(r.getParent());
            }
        });
    }

    private void initialiseCaches() {
        // The caches are organised as an array of maps
        // where the array stores the states of the grammar
        // as indices. So we have to get the biggest state
        // number.

        int indexSize = grammar.getAllStates().size();

        // initilize the arrays
        for (int i = 0; i <= indexSize; i++) {
            outsideCaches.add(new Object2DoubleOpenHashMap<>());
            insideCaches.add(new Object2DoubleOpenHashMap<>());
        }
    }

    private boolean isNullarySymbol(int state) {
        return nullarySymbols.contains(state);
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

    // Creates the cache for a given outside summary and all states
    // and saves it in the specified path.
    public void saveCaches(OutsideSummary outsideSummary, String path) {
        // precalculate
        grammar.getAllStates().forEach(state -> {
            estimateOutside(state, outsideSummary);
        });
        // save
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(insideCaches);
            out.writeObject(outsideCaches);
            out.close();
            fileOut.close();
        } catch (IOException i) {
            System.err.println("Could not save cache in '" + path + "'");
            i.printStackTrace(System.err);
        }
    }
    

    
    // Main method for testing purposes only 
    public static void main(String[] args) throws ParseException, IOException, ParserException {
        // <Caching tests>
        if (args.length != 3) {
            System.err.println("1. Path to IRTG, 2. Max length, 3. Path to write caches in.");
            System.exit(1);
        }
        String irtgFilename = args[0];
        
        // Create the IRTG from file
        System.err.println("irtgFilename: " + irtgFilename);
        InputCodec<InterpretedTreeAutomaton> codec = InputCodec.getInputCodecByExtension(Util.getFilenameExtension(irtgFilename));
        InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(irtgFilename)));
        
        // Initialize the AStarEstimator
        AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(new SXAlgebraStructureSummary(), irtg.getAutomaton());
        
        // Save as cache
        astar.saveCaches(new SXOutside(0, Integer.parseInt(args[1])), args[2]);
       
        // Read in again
//        AStarEstimator<String, Integer, SXOutside> astar2 = new AStarEstimator(estimator, irtg.getAutomaton(), args[2]);
        
        // <\ Caching test>

        // General tests: 
        /*
        if (args.length == 2) {
            String irtgFilename = args[0];
            String sentencesFilename = args[1];
            
            // Create the IRTG from file
            System.err.println("irtgFilename: " + irtgFilename);
            InputCodec<InterpretedTreeAutomaton> codec = new IrtgInputCodec();
            InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(irtgFilename)));
            
            // Create AlgebraStructureSummarizer
            SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
            
            // Initialize the AStarEstimator
            AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
            
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
                
//                int left = new Double(sentence_tok.length * 0.25).intValue();
//                int right = new Double(sentence_tok.length * 0.75).intValue();
//
//                SXOutside os = summarizer.summarizeOutside(new StringAlgebra.Span(left, right));
//                int stateos = irtg.getAutomaton().getIdForState("NP");
//                
//                System.err.println("Running for OS: " + os);
//  
//                
//                
//                
//                // Run the algorithm:
//                System.err.println("Result for " + irtg.getAutomaton().getStateForId(stateos) + " and outside summary " + os + ":\n  " + astar.estimateOutside(stateos, os));
                
                Integer is = summarizer.summarizeInside(new StringAlgebra.Span(0, sentence_tok.length));
                irtg.getAutomaton().getFinalStates().stream().forEach(state -> {
                
                    // Run the algorithm:
                    System.err.println("Result for " + irtg.getAutomaton().getStateForId(state) + " and inside summary " + is + ":\n  " + astar.estimateInside(state, is));

                
                });
                
                
            }
        } else {
            System.err.println("First argument: Path to IRTG. Second argument: Path to sentences");
            System.err.println("--> " + Arrays.toString(args));
            System.err.println("Len:" + args.length);

        }
        */
    }
}
