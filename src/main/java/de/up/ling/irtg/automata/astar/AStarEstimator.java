/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.InputCodec;
import de.up.ling.irtg.codec.ParseException;
import de.up.ling.irtg.util.MutableDouble;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the A*-Algorithm. Uses a grammar and an AlgebraStructureSummary 
 * to estimate an outside or inside score for a given grammar symbol and an inside/outside
 * summary. 
 * 
 * ### How to use the AStarEstimator:
 * 1. Needed classes
 *    - InterpretedTreeAutomaton that represents a grammar.
 *    - InsideSummary to represent values needed for the calculation
 *      of an inside estimate (e.g. an integer for string spans).
 *    - OutsideSummary to represent values needed for the calculation
 *      of an outside estimate (e.g. the start and end position for string spans).
 *    - AlgebraStructureSummary to find the next Inside/Outside objects
 *      during an Inside/Outside estimation.
 *    - AStarEstimator to estimate scores for grammar states and Inside/Outside 
 *      objects.
 *    - Summarizer to create InsideSummary and OutsideSummary objects
 *      depending on the input it was initialized with.
 * 
 * 2. Scopus
 *    While most objects are only created once per used algebra, 
 *    the Summarizer must be created for every new input sentence,
 *    since it can depend on the length of the input.
 * 
 * 3. Example (using string)
 *    // Read IRTG from file:
 *    InputCodec<InterpretedTreeAutomaton> codec = new IrtgInputCodec();
 *    InterpretedTreeAutomaton irtg = codec.read(new FileInputStream(new File(irtgFilename)));
 *    
 *    // Create the AlgebraStructureSummary:
 *    SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
 * 
 *    // Initialize the AStarEstimator (Integer is used as Inside-Class):
 *    AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
 * 
 *    // Create a Summarizer for each sentence:
 *    SXSummarizer summarizer = new SXSummarizer(Arrays.asList(sentence_as_list));
 *    
 *    // Example for an Outside-object and state:
 *    SXOutside os = summarizer.summarizeOutside(new StringAlgebra.Span(left, right));
 *    int state = irtg.getAutomaton().getIdForState("NP");
 *    
 *    // Estimate outside:
 *    double outsideEstimate = astar.estimateOutside(stateos, os);
 * 
 * 4. Adaption for other algebras:
 *    If you want to use the AStarEstimator with an algebra, that is not
 *    implemented yet, you have to implement the following interfaces:
 *    1. InsideSummary & OutsideSummary 
 *    2. Summarizer to create InsideSummary and OutsideSummary objects.
 *    3. AlgebraStructureSummary (probably the hardest part).
 * 
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <State>
 * @param <InsideSummary>
 * @param <OutsideSummary>
 */
public class AStarEstimator<State, InsideSummary, OutsideSummary> {
    private final AlgebraStructureSummary<InsideSummary, OutsideSummary> structureSummary;
    private final TreeAutomaton<State> grammar;
    private Int2ObjectMap<Set<Rule>> rhsSymbolToRules;  //< maps a symbol to a set of rules, where it occurs on the rhs
    private IntSet nullarySymbols;                      //< set of all symbols, that are the parent of a 0-ary rule.
    
    private final boolean DEBUG = false;

    // Caches
    private List<Object2DoubleMap<OutsideSummary>> outsideCaches; 
    private List<Object2DoubleMap<InsideSummary>> insideCaches;  

    /**
     * Default constructor that starts with an empty cache.
     * @param structureSummary
     * @param grammar 
     */
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> structureSummary, TreeAutomaton<State> grammar) {
        this.structureSummary = structureSummary;
        this.grammar = grammar;
        
        outsideCaches = new ArrayList<>();
        insideCaches = new ArrayList<>();

        sortRulesByRHS();
        collectNullarySymbols();
        initialiseCaches();
    }
    
    /**
     * Uses a precalculated cache from file.
     * @param estimator
     * @param grammar
     * @param cacheFile 
     */
    public AStarEstimator(AlgebraStructureSummary<InsideSummary, OutsideSummary> structureSummary, TreeAutomaton<State> grammar, String cacheFile) {
        this.structureSummary = structureSummary;
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
            if (structureSummary.isOutsideSummaryComplete(outsideSummary)) {
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
                        structureSummary.forEachRuleOutside(outsideSummary, r.getLabel(), r.getArity(), position, (OutsideSummary os, InsideSummary[] insideSummaries) -> {
                            
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
            if (structureSummary.isInsideSummaryTerminal(insideSummary)) {
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
                structureSummary.forEachRuleInside(insideSummary, r.getArity(),
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
    
    /**
     * Maps each symbol to a set of rules, where it appears in the rhs.
     * Crucial for the estimateOutside method.
     */
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

    /**
     * Create a set of symbols, that appear on the lhs of a 0-ary rule.
     */
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
    
}
