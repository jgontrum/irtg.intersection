/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.automata.condensed.EdgeEvaluator;

/**
 * The A*-EdgeEvaluator is used to return score for a state in 
 * the grammar (left) and the decomposition automaton (right).
 * The A*-Estimator calculates an (log) outside score and adds it to
 * the given (log) inside score.
 * 
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class AStarEdgeEvaluator implements EdgeEvaluator {
    private final AStarEstimator aStarEstimator;
    private final Summarizer summarizer;
    private final TreeAutomaton<StringAlgebra.Span> decompAutomaton;
    
    public AStarEdgeEvaluator(AStarEstimator aStarEstimator, Summarizer summarizer, TreeAutomaton<StringAlgebra.Span> decompAutomaton) {
        this.aStarEstimator = aStarEstimator;
        this.summarizer = summarizer;
        this.decompAutomaton = decompAutomaton;
    }
    
    @Override
    public double evaluate(int leftState, int rightState, double inside) {
        StringAlgebra.Span span = decompAutomaton.getStateForId(rightState);
                
        double outside = aStarEstimator.estimateOutside(leftState, summarizer.summarizeOutside(span));

        return inside + outside;
    }
    
}
