/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <InsideSummary>
 * @param <OutsideSummary>
 */
public interface Summarizer<State, InsideSummary, OutsideSummary> {
        
    double evaluate(State span, int state, int lengthOfInput);
    
    OutsideSummary summarizeOutside(State span, int length);
    
    InsideSummary summarizeInside(State span);
    
}
