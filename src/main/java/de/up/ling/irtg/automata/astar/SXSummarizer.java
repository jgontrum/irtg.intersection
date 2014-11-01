/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.algebra.StringAlgebra;
import java.util.List;


/**
 * Creates SX-Summary objects from Spans over an input string (StringAlgebra.Span)
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXSummarizer implements Summarizer<StringAlgebra.Span, SXInside, SXOutside>{
//    private final SXEstimator estimator;
    private final int length;
    
    public SXSummarizer(List<String> words) {
        length = words.size();
    }
    
    @Override
    public SXOutside summarizeOutside(StringAlgebra.Span span) {
        // summarizeOutside(3-5) = 3,2
        return new SXOutside(span.start, length - span.end);
    }

    @Override
    public SXInside summarizeInside(StringAlgebra.Span span) {
        // summarizeInside(3-5) = 2 
        return new SXInside(span.end - span.start);
    }
    
    
}
