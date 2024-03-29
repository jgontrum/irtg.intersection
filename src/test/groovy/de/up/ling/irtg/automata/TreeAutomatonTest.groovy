/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.up.ling.tree.*
import it.unimi.dsi.fastutil.ints.*
import de.up.ling.irtg.*
import de.up.ling.irtg.hom.*
import de.up.ling.irtg.algebra.*
import de.up.ling.irtg.signature.*
import com.google.common.collect.*
import static org.junit.Assert.*
import static de.up.ling.irtg.util.TestingTools.*;

/**
 *
 * @author koller
 */
class TreeAutomatonTest{
    static TreeAutomaton auto;
    static Signature sig;
    
    
    @Test
    public void testViterbi2() {
        TreeAutomaton auto = parse("""'DT1^NP2,0-1' -> r108 [1.0]
'S3,0-4'! -> r2414('NP2^S3,0-2', 'ART-VP1-.1^S3,2-4') [1.0]
'ART-.1^S3,3-4' -> r27('.1^S3,3-4') [1.0]
'NP2^S3,0-2' -> r521('DT1^NP2,0-1', 'NN1^NP2,1-2') [1.0]
'.1^S3,3-4' -> r26 [1.0]
'VP1^S3,2-3' -> r347('VBD1^VP1,2-3') [1.0]
'ART-VP1-.1^S3,2-4' -> r348('VP1^S3,2-3', 'ART-.1^S3,3-4') [1.0]
'VBD1^VP1,2-3' -> r3544 [1.0]
'NN1^NP2,1-2' -> r3501 [1.0]""");
        
        Tree best = auto.viterbi();
        assertEquals(pt("r2414(r521(r108,r3501),r348(r347(r3544),r27(r26)))"), best);
    }
    
    private static setAutomaton(String s) {
        auto = parse(s);
        sig = auto.getSignature();
    }
    
    @Test
    public void testEqualsWithSignatures() {
        TreeAutomaton auto1 = parse("q1 -> a\n q2 ! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("q2 ! -> f(q1,q1)\n q1 -> a ");
        
        assert auto1.equals(auto2);
    }
    
    
    @Test
    public void testNotEqualsWithSignatures() {
        TreeAutomaton auto1 = parse("q1 -> b\n q2 ! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("q2 ! -> f(q1,q1)\n q1 -> a ");
        
        assert ! auto1.equals(auto2);
    }


    @Test
    public void testNotEquals2WithSignatures() {
        TreeAutomaton auto1 = parse("q1 -> a\n q2 ! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("q2 ! -> f(q,q)\n q -> a ");
        
        assert ! auto1.equals(auto2);
    }
    
    @Test
    public void testIntersection() {
        TreeAutomaton auto1 = parse("q1 -> a\n q2 ! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a  \n p3 -> a");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        assert intersect.getSignature() == auto1.getSignature();
        
        assertEquals(intersect.getSignature().getSymbolsWithArities(), ["f":2, "a":0]);
        
        assertEquals(new HashSet([rs(p("q1","p2"), "a", [], intersect), rs(p("q1", "p3"), "a", [], intersect)]), 
            rbu("a", [], intersect));

        assertEquals(new HashSet([rs(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")], intersect)]), 
            rbu("f", [p("q1","p2"), p("q1","p3")], intersect));
        
        assertEquals(new HashSet([p("q2","p1")]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it)}));
    }
    
    
    
    @Test
    public void testIntersectionLanguage() {
        
        TreeAutomaton auto1 = parse("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p2)\n p2 -> a");
        
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        assertEquals(new HashSet([pt("f(a,a)")]), intersect.language());
    }
    
    private Set<Rule> rbu(String label, List children, TreeAutomaton auto) {
        return auto.getRulesBottomUp(auto.getSignature().getIdForSymbol(label), children.collect { auto.getIdForState(it)});
    }

    
    @Test
    public void testIntersectionLanguageEarley() {
        TreeAutomaton auto1 = parse("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p2)\n p2 -> a");
        TreeAutomaton intersect = auto1.intersectEarley(auto2);
        
        assertEquals(intersect.language(), new HashSet([pt("f(a,a)")]));
    }
    
    @Test
    public void testIntersectionFinalStates() {
        TreeAutomaton auto1 = parse("q1! -> f(q2, q3)\n q2 -> a\n q3 -> a\n q3 -> b\n qqqq! -> xxxx");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p2)\n p2 -> a");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        assertEquals(new HashSet(["q1,p1"]), new HashSet(intersect.getFinalStates().collect { intersect.getStateForId(it).toString() }));
    }
    
    @Test
    public void testAsConcrete() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0]), rhs.getSignature());
                            
        TreeAutomaton pre = rhs.inverseHomomorphism(h);        
        
        assertEquals(pre, pre.asConcreteTreeAutomaton());
    }
    

    
    @Test
    public void testInvHom() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0]), rhs.getSignature());

        TreeAutomaton gold = parse("q13 -> r1(q12,q23) \n q24 -> r1(q23,q34) \n q14! -> r1(q12,q24) \n" +
                "q14 -> r1(q13,q34) \n q13 -> r2(q12,q23) \n q24 -> r2(q23,q34) \n q14! -> r2(q12,q24) \n" +
                "q14 -> r2(q13,q34) \n q12 -> r3  \n q23 -> r4  \n q34 -> r5 ");

        TreeAutomaton pre = rhs.inverseHomomorphism(h);        
        pre.makeAllRulesExplicit();
        
//        assertEquals(gold, pre); //TODO FIXTHIS
    }
    
    @Test
    public void testInvHomNonlinearBottomUp() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]), rhs.getSignature())
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        // don't do anything here that would trigger computation of top-down rules
        assert pre.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomNonlinearTopDown() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]), rhs.getSignature())
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit(); // this triggers computing all top-down rules
        
        assert pre.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomNonlinearByIntersection() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["G":"f(?1)", "A":"a"], sig(["G":2, "A":0]), rhs.getSignature());
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        
        TreeAutomaton left = parse("p2! -> G(p1,p1) \n p1 -> A"); // accepts { G(A,A) }
        TreeAutomaton result = left.intersect(pre)
        
        assert result.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomNondeleting() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0]), rhs.getSignature() );

        TreeAutomaton gold = parse("q13 -> r1(q12,q23) \n q24 -> r1(q23,q34) \n q14! -> r1(q12,q24) \n" +
                "q14 -> r1(q13,q34) \n q13 -> r2(q12,q23) \n q24 -> r2(q23,q34) \n q14! -> r2(q12,q24) \n" +
                "q14 -> r2(q13,q34) \n q12 -> r3  \n q23 -> r4  \n q34 -> r5 ");

        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit();
        
        assertEquals( gold.language(), pre.language() );
    }
    
    @Test
    public void testInvHomNdDifferentSignature() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r3":"a", "r4":"b", "r5":"d", "r1":"c(?1,?2)", "r2":"c(?1,?2)"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0])); // fresh target signature
        
        TreeAutomaton gold = parse("q13 -> r1(q12,q23) \n q24 -> r1(q23,q34) \n q14! -> r1(q12,q24) \n" +
                "q14 -> r1(q13,q34) \n q13 -> r2(q12,q23) \n q24 -> r2(q23,q34) \n q14! -> r2(q12,q24) \n" +
                "q14 -> r2(q13,q34) \n q12 -> r3  \n q23 -> r4  \n q34 -> r5 ");

        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit();
        
        assertEquals( gold.language(), pre.language() );
    }
    
    
    // TODO - currently this test fails. It also doesn't test what it was supposed to, because "intersect"
    // now goes through the right automaton top-down, but the test should go through it bottom-up.
    // Once the intersection algorithm stabilizes, we need to reactivate this test.
//    @Test
    public void testInvHomNdDifferentSignatureBottomUp() {
        TreeAutomaton rhs = parse("q13 -> c(q12,q23) \n q24 -> c(q23,q34) \n q14! -> c(q12,q24) \n" +
                "q14 -> c(q13,q34) \n q12 -> a \n q23 -> b \n q34 -> d ");
        Homomorphism h = hom(["r3":"a", "r4":"b", "r5":"d", "r1":"c(?1,?2)", "r2":"c(?1,?2)"], 
                                sig(["r1":2, "r2":2, "r3":0, "r4":0, "r5":0])); // fresh target signature
        
        TreeAutomaton gold = parse("q13 -> r1(q12,q23) \n q24 -> r1(q23,q34) \n q14! -> r1(q12,q24) \n" +
                "q14 -> r1(q13,q34) \n q13 -> r2(q12,q23) \n q24 -> r2(q23,q34) \n q14! -> r2(q12,q24) \n" +
                "q14 -> r2(q13,q34) \n q12 -> r3  \n q23 -> r4  \n q34 -> r5 ");
        
        TreeAutomaton invhom = rhs.inverseHomomorphism(h);
        
        assert invhom instanceof NondeletingInverseHomAutomaton;

        TreeAutomaton pre =  new UniversalAutomaton(h.getSourceSignature()).intersect(invhom);
        
        assertEquals( gold.language(), pre.language() );
    }
    
    @Test
    public void testInvHomDeletingDifferentSignatureTopDown() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["A":"a", "G":"f(?1)"], sig(["G":2, "A":0])); // fresh target signature
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        pre.toString(); // triggers computation of top-down rules
        assert pre.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomDeletingDifferentSignatureBottomUp() {
        TreeAutomaton rhs = parse("q2! -> f(q1) \n q1 -> a "); // accepts { f(a) }
        Homomorphism h = hom(["A":"a", "G":"f(?1)"], sig(["G":2, "A":0])); // fresh target signature
        
        TreeAutomaton pre = rhs.inverseHomomorphism(h);
        // don't do anything here that would trigger computation of top-down rules
        assert pre.accepts(pt("G(A,A)"));
    }
    
    @Test
    public void testInvHomMarco() {
        Signature s = sig(["a":0, "F":3]);
        Homomorphism h = hom(["a":"a", "F":"*(*(?1,?2),?3)"], s);
        
        StringAlgebra alg = new StringAlgebra();
        TreeAutomaton decomp = alg.decompose(alg.parseString("a a a"));
        
        TreeAutomaton pre = decomp.inverseHomomorphism(h);
        
        assertEquals(new HashSet([pt("F(a,a,a)")]), new HashSet(pre.language()));
    }
    
    @Test
    public void testInvhomNonConcrete() {
        InterpretedTreeAutomaton irtg = pi(AB_IRTG);
        Homomorphism h = irtg.getInterpretation("i").getHomomorphism();
        Algebra a = irtg.getInterpretation("i").getAlgebra();

        TreeAutomaton decomp = a.decompose(a.parseString("a a a"));

//        System.err.println("=====================");
//        System.err.println(decomp);
        
        
        TreeAutomaton inv = decomp.inverseHomomorphism(h);
        
//        System.err.println("++++++++++++++++");
//        System.err.println(inv);
        
//        System.err.println(inv.getSignature())
        
//        System.err.println("inv language => " + inv.language())
        
        assertEquals(new HashSet([pt("r1(r1(r2,r2), r2)"), pt("r1(r2, r1(r2,r2))")]), inv.language())
    }
    
    @Test
    public void testHom() {
        TreeAutomaton base = parse("q! -> f(q1, q2)\n q -> g(q1, q2)\n q -> h(q1,q2)\n q1 -> a \n q2 -> b ");
        Signature s = new Signature()
        Homomorphism h = hom(["f":"H(F(?1,?2))", "g":"H(F(?1,?2))", "h":"G(?2,?1)", "a":"A", "b":"B"], base.getSignature(), s);
        Set gold = new HashSet([pt("H(F(A,B))"), pt("G(B,A)")])
        
        TreeAutomaton result = base.homomorphism(h)
                
        assertEquals(gold, new HashSet(result.language()));
    }
    
    @Test
    public void testHomOneVariable() {
        TreeAutomaton base = parse("q1! -> f(q2) \n q2 -> a");
        TreeAutomaton gold = parse("q1! -> A \n q2 -> A ");
        Homomorphism h = hom(["f": "?1", "a": "A"], base.getSignature(), gold.getSignature())
        
        TreeAutomaton result = base.homomorphism(h)
        
        assertEquals(new HashSet([pt("A")]), result.language());
    }

    @Test
    public void testViterbi() {
        TreeAutomaton auto = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1) [1]\n q! -> f(q1,q2) [1.5]");
        Tree best = auto.viterbi();
        assertEquals(best, pt("f(a,a)"));
    }
    

    @Test
    public void testInside() {
        TreeAutomaton auto = parse("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        Map inside = auto.inside();
        assertEquals(7.0, inside.get(auto.getIdForState("q")), 0.001);
        assertEquals(2.0, inside.get(auto.getIdForState("q1")), 0.001);
    }

    @Test
    public void testOutside() {
        TreeAutomaton auto = parse("q1 -> a  [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        Map inside = auto.inside();
        Map outside = auto.outside(inside);
        assertEquals(1.0, outside.get(auto.getIdForState("q")), 0.001);
        assertEquals(5.5, outside.get(auto.getIdForState("q1")), 0.001);
    }
    
    @Test
    public void testLanguage() {
        setAutomaton("q1 -> a [2]\n q2 -> b [1]\n q! -> f(q1,q1)  [1]\n q! -> f(q1,q2) [1.5]");
        
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet([pt("f(a,a)"), pt("f(a,b)")]*.toString());
        assertEquals(gold, lang);
    }
    
    @Test
    public void testLanguage2() {
        setAutomaton("q1 -> a\n q1 -> b\n q2 -> c\n q2 -> d\n q! -> f(q1,q2)\n q! -> g(q1,q2)");
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        assertEquals(gold, lang)
    }
    
    @Test
    public void testLanguageIterator() {
        setAutomaton("q1 -> a\n q1 -> b\n q2 -> c\n q2 -> d\n q! -> f(q1,q2)\n q! -> g(q1,q2)");
        Set lang = new HashSet();
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        
        for( Tree t : auto.languageIterable() ) {
            lang.add(t.toString())
        }
        
        assertEquals(gold, lang)
    }
    
    @Test
    public void testEmptyLanguageIterator() {
        setAutomaton("q! -> g(q1,q2)");
        Set lang = new HashSet();
        Set gold = new HashSet();
        
        for( Tree t : auto.languageIterable() ) {
            lang.add(t.toString())
        }
        
        assertEquals(gold, lang)
    }
    
    @Test
    public void testReduce() {
        TreeAutomaton auto = parse("""P.4-5 -> r12 [1.0]
N.6-7 -> r11 [1.0]
N.3-4 -> r10  [1.0]
PP.4-7 -> r6(P.4-5, NP.5-7)  [1.0]
NP.0-1 -> r7 [1.0]
V.1-2 -> r8  [1.0]
S.0-4 -> r1(NP.0-1, VP.1-4) [1.0]
S.0-7! -> r1(NP.0-1, VP.1-7)  [1.0]
Det.2-3 -> r9  [1.0]
Det.5-6 -> r9  [1.0]
NP.5-7 -> r2(Det.5-6, N.6-7) [1.0]
NP.2-4 -> r2(Det.2-3, N.3-4) [1.0]
NP.2-7 -> r2(Det.2-3, N.3-7) [1.0]
N.3-7 -> r3(N.3-4, PP.4-7) [1.0]
VP.1-4 -> r4(V.1-2, NP.2-4) [1.0]
VP.1-7  -> r4(V.1-2, NP.2-7) [1.0]
VP.1-7 -> r5(VP.1-4, PP.4-7) [1.0]""");
        
        Set productiveStates = new HashSet(auto.getReachableStates().collect { auto.getStateForId(it)});
        Set gold = new HashSet(["NP.0-1", "V.1-2", "Det.2-3", "N.3-4", "P.4-5", "Det.5-6", "N.6-7", "PP.4-7", "S.0-7", "NP.5-7", "NP.2-4", "NP.2-7", "N.3-7", "VP.1-4", "VP.1-7"]);
        assertEquals(gold, productiveStates);        
    }
    
    @Test
    public void testReduceUnreachableFinalStates() {
        TreeAutomaton auto = parse("""q! -> a\n qqq -> b""");
        auto.addFinalState(auto.addState("qx"));
        
        TreeAutomaton red = auto.reduceTopDown();
        
        assert red.getFinalStates().contains(auto.addState("q"));
        assert red.getFinalStates().contains(auto.addState("qx"));
        assert ! red.getAllStates().contains(auto.getIdForState("qqq"));
    }
    
    @Test
    public void testIntersectWeights() {
        TreeAutomaton auto1 = parse("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w-0.3) < 0.001) || (Math.abs(w-0.2) < 0.001)) : rulesForA;
        }
        
//        assertEquals( new HashSet([0.4*0.5, 0.6*0.5]), new HashSet(rulesForA.collect { it.getWeight() }) )
    }
    
    @Test
    public void testIntersectWeightsViaExplicit() {
        TreeAutomaton auto1 = parse("q1 -> a  [0.5]\n q2! -> f(q1,q1) ");
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        TreeAutomaton intersect = auto1.intersect(auto2);
        
        intersect.makeAllRulesExplicit();        
        Set rulesForA = rbu("a", [], intersect);
        
        for( Rule r : rulesForA ) {
            double w = r.getWeight();
            assert ((Math.abs(w)-0.3 < 0.001) || (Math.abs(w)-0.2 < 0.001)) : rulesForA;
        }
    }
    
    @Test
    public void testBottomUpDeterministic1() {
        _testBottomUpDeterministic("p1! -> f(p2,p3) \n p2 -> a\n p3 -> a", false);
    }

    @Test
    public void testBottomUpDeterministic2() {
        _testBottomUpDeterministic("p1! -> f(p2,p3) \n p2 -> a\n p3 -> b", true);
    }
    
    private void _testBottomUpDeterministic(String auto, boolean gold) {
        assert pa(auto).isBottomUpDeterministic() == gold;
    }
    
    
    private Set iterableToSet(Iterable it) {
        Set ret = new HashSet();
        Iterables.addAll(ret, it);
        return ret;
    }
    
    @Test
    public void testRun() {
        TreeAutomaton auto2 = parse("p1! -> f(p2,p3) \n p2 -> a\n p3 -> a");
        
        Tree t = pt("f(a,a)");
        assertEquals(new HashSet(["p1"]), iterableToSet(auto2.run(t)));

        Tree ta = pt("a");
        assertEquals(new HashSet(["p2","p3"]), iterableToSet(auto2.run(ta)));
    }
    
    @Test
    public void testRunWeights() {
        setAutomaton("p1! -> f(p2,p3)\n p2 -> a [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(0.24 - auto.getWeight(t)) < 0.001;
    }    
    
    @Test
    public void testRunWeightsSumAtRoot() {
        setAutomaton("p1! -> f(p2,p3) \n p1! -> f(p2,p2) \n p2 -> a  [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(0.4 - auto.getWeight(t)) < 0.001;
    }
    
    @Test
    public void testRunWeightsSumInMiddle() {
        setAutomaton("p0! -> g(p1) \n p1 -> f(p2,p3) \n p1 -> f(p2,p2) \n p2 -> a [0.4]\n p3 -> a  [0.6]");
        Tree t = pt("g(f(a,a))");
        
        assert Math.abs(0.4 - auto.getWeight(t)) < 0.001;
    }

    @Test
    public void testRunWeightsNotInLanguage() {
        setAutomaton("p0! -> g(p1) \n p1 -> f(p2,p3) \n p1 -> f(p2,p2) \n p2 -> a [0.4]\n p3 -> a [0.6]");
        Tree t = pt("f(a,a)");
        
        assert Math.abs(auto.getWeight(t)) < 0.001;
    }
    
    @Test
    public void testRun1() {
        _testRun("p1! -> f(p2,p3) \n p2 -> a\n p3 -> a",  "f(a,a)", ["p1"]); // nondet
    }

    @Test
    public void testRun2() {
        _testRun("p1! -> f(p2,p3) \n p2 -> a\n p3 -> a", "a", ["p2", "p3"]); // nondet
    }
    
    @Test
    public void testRun3() {
        _testRun("p1! -> f(p2,p3) \n p2 -> a\n p3 -> b", "f(a,b)", ["p1"]); // deterministic
    }
    
    private void _testRun(String auto, String tree, List gold) {
        TreeAutomaton a = pa(auto);
        assert iterableToSet(a.run(pt(tree))).equals(new HashSet(gold));
    }
    
    
    @Test
    public void testConcreteDeterministic() {
        TreeAutomaton auto1 = parse("q1 -> a \n q2 -> a");
        assert auto1 instanceof ConcreteTreeAutomaton;
        assert ! auto1.isBottomUpDeterministic();
    }
    
    @Test
    public void testCyclic() {
        TreeAutomaton a = parse("q1! -> f(q2,q3)\n q2 -> g(q3)\n q3 -> g(q1)");
        assert a.isCyclic();
    }
    
    @Test
    public void testAcyclic() {
        TreeAutomaton a = parse("q1! -> f(q2,q3)\n q2 -> g(q3)\n q3 -> a");
        assert ! a.isCyclic();
    }
    
    @Test
    public void testGetBottomUpCyclic() {
        TreeAutomaton a = parse("q1! -> f(q2,q3)\n q2 -> g(q1)\n q1 -> a\n q3 -> b");
    }
    
    @Test
    public void testRuleTree() {
        TreeAutomaton a = parse("s03! -> f(s01,s13)\n s03 -> f(s02,s23)\n s13 -> f(s12, s23)\n s02 -> f(s01, s12)\n s01 -> a\n s12 -> a\n s23 -> a");
        Tree dt = a.getSignature().addAllSymbols(pt("f(f(a,a),a)"));
        Tree result = a.getRuleTree(dt);
        Tree gold = Tree.create(rs("s03", "f", ["s02","s23"], a),
            [Tree.create(rs("s02", "f", ["s01", "s12"], a),
                    [Tree.create(rs("s01", "a", [], a)), Tree.create(rs("s12", "a", [], a))]),
             Tree.create(rs("s23", "a", [], a))]);
     
        assertEquals(gold, result)
    }
    
    @Test
    public void testRuleIterable() {
        TreeAutomaton a = parse("s03! -> f(s01,s13)\n s03 -> f(s02,s23)\n s13 -> f(s12, s23)\n s02 -> f(s01, s12)\n s01 -> a\n s12 -> a\n s23 -> a");
        
        assertEquals(h(a.getRuleSet()), h(a.getRuleIterable()));
        assertEquals(h(a.getRuleSet()), h(a.getRuleIterable())); // can use iterable twice
    }
    
    @Test
    public void testRuleIterator() {
        TreeAutomaton a = parse("s03! -> f(s01,s13)\n s03 -> f(s02,s23)\n s13 -> f(s12, s23)\n s02 -> f(s01, s12)\n s01 -> a\n s12 -> a\n s23 -> a");
        
        Set fromIterator = new HashSet()
        Iterators.addAll(fromIterator, a.getRuleIterator());
        
        assertEquals(h(a.getRuleSet()), fromIterator);
    }
    
//    @Test
    public void testBottomUpOrder() {
        TreeAutomaton a = parse("s03! -> f(s01,s13)\n s03 -> f(s02,s23)\n s13 -> f(s12, s23)\n s02 -> f(s01, s12)\n s01 -> a\n s12 -> a\n s23 -> a");
        List states = a.getStatesInBottomUpOrder().collect { a.getStateForId(it)};
        
        assertEquals(["s01", "s12", "s23", "s02", "s13", "s03"], states)
    }
    
    
    /*
    def "testing whether automaton is bottom-up deterministic"() {
        expect:
        pa(automaton).isBottomUpDeterministic() == gold
        
        where:
        automaton                              | gold
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> a" | false
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> b" | true
    }
    
    def "running automaton on tree"() {
        expect:
        pa(automaton).run(pt(tree)).equals(new HashSet(gold))
        
        where:
        automaton                                 | tree        | gold
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> a"    | "f(a,a)"    | ["p1"]           // nondet
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> a"    | "a"         | ["p2", "p3"]     // nondet
        "p1! -> f(p2,p3) \n p2 -> a\n p3 -> b"    | "f(a,b)"    | ["p1"]           // deterministic
        
    }
    */
    
    private static Set h(Iterable x) {
        Set ret = new HashSet();
        Iterables.addAll(ret, x);
        return ret;
    }
    

    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }

    private static TreeAutomaton parse(String s) {
        return TreeAutomatonParser.parse(new StringReader(s));
    }

//    private static Rule r(parent, label, children) {
//        return new Rule(parent, label, children);
//    }
    
    private static Rule rs(parent, String label, children, TreeAutomaton automaton) {
        return automaton.createRule(parent, label, children, 1);
    }
    
    private static Tree<Integer> ptii(String s) {
        return pti(s, sig);
    }
    
    private static final String AB_IRTG = """
interpretation i: de.up.ling.irtg.algebra.StringAlgebra

S! -> r1(S,S)
   [i] *(?1,?2)

S -> r2
   [i] a

S -> r3
   [i] b
""";
}

