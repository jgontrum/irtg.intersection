/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.algebra.StringAlgebra;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 * @param <InsideSummary>
 * @param <OutsideSummary>
 */
public interface Summarizer<InsideSummary, OutsideSummary> {
        
    double evaluate(StringAlgebra.Span span, int state, int lengthOfInput);
    
    OutsideSummary summarizeOutside(StringAlgebra.Span span, int length);
    
    InsideSummary summarizeInside(StringAlgebra.Span span);
    
}
