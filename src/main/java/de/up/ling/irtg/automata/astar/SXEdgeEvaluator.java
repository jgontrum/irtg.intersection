/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXEdgeEvaluator implements EdgeEvaluator {
    private final AStarEstimator aStarEstimator;
    private final Summarizer summarizer;
    private final TreeAutomaton<StringAlgebra.Span> decompAutomaton;
    
    public SXEdgeEvaluator(AStarEstimator aStarEstimator, Summarizer summarizer, TreeAutomaton<StringAlgebra.Span> decompAutomaton) {
        this.aStarEstimator = aStarEstimator;
        this.summarizer = summarizer;
        this.decompAutomaton = decompAutomaton;
    }
    
    @Override
    public double evaluate(int leftState, int rightState) {
        StringAlgebra.Span span = decompAutomaton.getStateForId(rightState);
        
        double inside = aStarEstimator.estimateInside(leftState, summarizer.summarizeInside(span));
        
        double outside = aStarEstimator.estimateOutside(leftState, summarizer.summarizeOutside(span));

        return inside + outside;
    }
    
}
