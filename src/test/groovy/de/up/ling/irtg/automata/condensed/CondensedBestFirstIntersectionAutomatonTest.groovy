/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.condensed

import de.up.ling.irtg.InterpretedTreeAutomaton
import de.up.ling.irtg.algebra.Algebra
import de.up.ling.irtg.automata.TreeAutomaton
import de.up.ling.irtg.automata.astar.AStarEdgeEvaluator
import de.up.ling.irtg.automata.astar.AStarEstimator
import de.up.ling.irtg.automata.astar.AlgebraStructureSummary
import de.up.ling.irtg.automata.astar.SXAlgebraStructureSummary
import de.up.ling.irtg.automata.astar.SXOutside
import de.up.ling.irtg.automata.astar.SXSummarizer
import de.up.ling.irtg.automata.astar.Summarizer
import de.up.ling.irtg.hom.Homomorphism
import de.up.ling.irtg.signature.IdentitySignatureMapper
import org.junit.Test
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
class CondensedBestFirstIntersectionAutomatonTest {
	        String grammarstring = '''
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(A, B)
 [i] *(?1, ?2)

A -> r2 
 [i] a

B -> r3(A, D)
 [i] *(?1, ?2)

D -> r5(A, D) [0.5]
 [i] *(?1, ?2)

D -> r4 [0.5]
 [i] b

C -> r6(X,Y)
 [i] *(?1, ?2)

X -> r7
  [i] x

Y -> r8(X,X)
[i] *(?1, ?2)


       ''';
    @Test
    public void intersectionTest() {
         // Create an IRTG
        InterpretedTreeAutomaton irtg = pi(grammarstring);
        String toParse = "a a a a b ";
        
        System.out.println("Parsing the String '" + toParse + "' with the IRTG: \n" + irtg.toString());
        
        // Get Homomorphism and Algebra
        Homomorphism hom = irtg.getInterpretation("i").getHomomorphism();
        Algebra alg = irtg.getInterpretation("i").getAlgebra();
        
        TreeAutomaton decomp = alg.decompose(alg.parseString(toParse));
        CondensedTreeAutomaton inv = decomp.inverseCondensedHomomorphism(hom);
        
        AlgebraStructureSummary<Integer, SXOutside> structureSummarizer = new SXAlgebraStructureSummary();
        AStarEstimator<String, Integer, SXOutside> astar = new AStarEstimator(structureSummarizer, irtg.getAutomaton());
        
        
        Summarizer summarizer = new SXSummarizer(Arrays.asList(toParse.split(" ")));
        EdgeEvaluator edgeEvaluator = new AStarEdgeEvaluator(astar, summarizer, decomp);

        TreeAutomaton result = new CondensedBestFirstIntersectionAutomaton<>(
                                irtg.getAutomaton(),
                                inv,
                                new IdentitySignatureMapper(irtg.getAutomaton().getSignature()),
                                edgeEvaluator
                        );
        result.makeAllRulesExplicit();
        
        System.err.println(result);
 
    }
   
}

