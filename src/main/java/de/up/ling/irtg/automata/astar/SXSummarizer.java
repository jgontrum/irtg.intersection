/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.ParserException;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IrtgInputCodec;
import de.up.ling.irtg.codec.ParseException;
import java.io.IOException;


/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXSummarizer implements Summarizer<StringAlgebra.Span, SXInside, SXOutside>{
    private final SXEstimator estimator;
    
    public SXSummarizer(SXEstimator estimator) {
        this.estimator = estimator;
    }
    
    @Override
    public double evaluate(StringAlgebra.Span span, int state, int lengthOfInput) {
        return evaluate(state, summarizeOutside(span, lengthOfInput));
    }
    
    public double evaluate(int state, SXOutside os) {
        System.err.println("SXOutside: " + os);
        return estimator.estimateOutside(state, os);
    }
    
    @Override
    public SXOutside summarizeOutside(StringAlgebra.Span span, int length) {
        // summarizeOutside(3-5) = 3,2
        return new SXOutside(span.start, length - span.end);
    }

    @Override
    public SXInside summarizeInside(StringAlgebra.Span span) {
        // summarizeInside(3-5) = 2 
        return new SXInside(span.end - span.start);
    }


    // Main method for testing purposes only 
    public static void main(String[] args) throws ParseException, IOException, ParserException {
        String scfgIRTG  = "interpretation english: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "interpretation german: de.up.ling.irtg.algebra.StringAlgebra\n"
            + "\n"
            + "\n"
            + "S! -> r1(NP,VP)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "\n"
            + "NP -> r2(Det,N)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "N -> r3(N,PP)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "VP -> r4(V,NP)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "VP -> r5(VP,PP)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "PP -> r6(P,NP)\n"
            + "  [english] *(?1,?2)\n"
            + "  [german] *(?1,?2)\n"
            + "\n"
            + "NP -> r7\n"
            + "  [english] john\n"
            + "  [german] hans\n"
            + "\n"
            + "V -> r8\n"
            + "  [english] watches\n"
            + "  [german] betrachtet\n"
            + "\n"
            + "Det -> r9\n"
            + "  [english] the\n"
            + "  [german] die\n"
            + "\n"
            + "Det -> r9b\n"
            + "  [english] the\n"
            + "  [german] dem\n"
            + "\n"
            + "N -> r10\n"
            + "  [english] woman\n"
            + "  [german] frau\n"
            + "\n"
            + "N -> r11\n"
            + "  [english] telescope\n"
            + "  [german] fernrohr\n"
            + "\n"
            + "P -> r12\n"
            + "  [english] with\n"
            + "  [german] mit\n";
        InterpretedTreeAutomaton irtg = new IrtgInputCodec().read(scfgIRTG);
        
        String input = "der mann beobachtet die frau mit dem fernrohr";
        
        SXEstimator estimator = new SXEstimator(irtg.getAutomaton());
        
        Summarizer summarizer = new SXSummarizer(estimator);
        
        irtg.getAutomaton().getFinalStates().forEach((finalState) -> {
            System.err.println("finalState: \t"+finalState);
            System.err.println(summarizer.evaluate(new StringAlgebra.Span(2, 3), finalState, 8));
        });
        
    }
    
}
