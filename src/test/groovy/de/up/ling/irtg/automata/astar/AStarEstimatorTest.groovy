/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar


import org.junit.Test
import java.util.*
import java.io.*
import de.up.ling.irtg.automata.*
import static org.junit.Assert.*
import de.up.ling.tree.*;
import de.up.ling.irtg.algebra.*;
import de.up.ling.irtg.hom.*;
import de.up.ling.irtg.corpus.*
import static de.up.ling.irtg.util.TestingTools.*;
import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.automata.condensed.CondensedTreeAutomaton

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
class AStarEstimatorTest {

    @Test
    public void estimateOutsideTest() {
        InterpretedTreeAutomaton irtg = pi(CFG);
        String stringInput = "john watches the woman with the telescope"
        
        String[] stringInput_tok = stringInput.split(" ");
        
        SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
        AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
        SXSummarizer summarizer = new SXSummarizer(Arrays.asList(stringInput_tok));
                
        int state1 = irtg.getAutomaton().getIdForState("NP");
        int state2 = irtg.getAutomaton().getIdForState("S");

        SXOutside os1 = summarizer.summarizeOutside(new StringAlgebra.Span(1, 1));
        SXOutside os2 = summarizer.summarizeOutside(new StringAlgebra.Span(0, 7));
        
        assert astar.estimateOutside(state1, os1) == Double.NEGATIVE_INFINITY;
        assert astar.estimateOutside(state2, os2) == 0.0;

    }
    
    
    @Test
    public void estimateInsideTest() {
        InterpretedTreeAutomaton irtg = pi(CFG);
        String stringInput = "john watches the woman with the telescope"
        
        String[] stringInput_tok = stringInput.split(" ");
        
        SXAlgebraStructureSummary estimator = new SXAlgebraStructureSummary();
        AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(estimator, irtg.getAutomaton());
        SXSummarizer summarizer = new SXSummarizer(Arrays.asList(stringInput_tok));
                
        int state = irtg.getAutomaton().getIdForState("NP");

        assert astar.estimateInside(state, 1) == -0.10536051565782628;
        assert astar.estimateInside(state, 7) == -4.9212517329615695;
    }
    
    
    private static final String CFG = '''\n\
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(NP,VP) [1.0]
  [i] *(?1,?2)


VP -> r4(V,NP) [0.3]
  [i] *(?1,?2)


VP -> r5(VP,PP) [0.7]
  [i] *(?1,?2)


PP -> r6(P,NP) [1.0]
  [i] *(?1,?2)


NP -> r7 [0.1] 
  [i] john


NP -> r2(Det,N) [0.9]
  [i] *(?1,?2)


V -> r8 [1.0]
  [i] watches


Det -> r9 [1.0]
  [i] the


N -> r10 [0.3]
  [i] woman


N -> r11 [0.5]
  [i] telescope

N -> r3(N,PP) [0.2]
  [i] *(?1,?2)

P -> r12 [1.0]
  [i] with
''';
    
}

