/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.irtg.automata


import org.junit.*
import java.util.*
import java.io.*
import de.saar.basic.*
import de.saar.chorus.term.parser.*
import de.saar.basic.tree.*
import de.saar.penguin.irtg.hom.*
import static org.junit.Assert.*
import static de.saar.penguin.irtg.hom.HomomorphismTest.hom;


/**
 *
 * @author koller
 */
class BottomUpAutomatonTest {
    @Test
    public void testIntersection() {
        BottomUpAutomaton auto1 = parse("a -> q1\n f(q1,q1) -> q2 !");
        BottomUpAutomaton auto2 = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");
        BottomUpAutomaton intersect = auto1.intersect(auto2);

        assertEquals(new HashSet([r(p("q1","p2"), "a", []), r(p("q1", "p3"), "a", [])]), 
            intersect.getRulesBottomUp("a", []));

        assertEquals(new HashSet([r(p("q2", "p1"), "f", [p("q1","p2"), p("q1","p3")])]), 
            intersect.getRulesBottomUp("f", [p("q1","p2"), p("q1","p3")]));

        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
        assertEquals(new HashSet([p("q2","p1")]), intersect.getFinalStates());
    }

    @Test
    public void testRun() {
        BottomUpAutomaton auto2 = parse("f(p2,p3) -> p1!\n a -> p2\n a -> p3");

        Tree t = TermParser.parse("f(a,a)").toTree();
        assertEquals(new HashSet(["p1"]), auto2.run(t));

        Tree ta = TermParser.parse("a").toTree();
        assertEquals(new HashSet(["p2","p3"]), auto2.run(ta));
    }

    @Test
    public void testInvHom() {
        BottomUpAutomaton rhs = parse("c(q12,q23) -> q13\n c(q23,q34) -> q24\n c(q12,q24) -> q14!\n" +
                "c(q13,q34) -> q14\n a -> q12\n b -> q23\n d -> q34");
        Homomorphism h = hom(["r1":"c(?1,?2)", "r2":"c(?1,?2)", "r3":"a", "r4":"b", "r5":"d"]);

        BottomUpAutomaton gold = parse("r1(q12,q23) -> q13\n r1(q23,q34) -> q24\n r1(q12,q24) -> q14!\n" +
                "r1(q13,q34) -> q14\n r2(q12,q23) -> q13\n r2(q23,q34) -> q24\n r2(q12,q24) -> q14!\n" +
                "r2(q13,q34) -> q14\n r3 -> q12\n r4 -> q23\n r5 -> q34");

        BottomUpAutomaton pre = rhs.inverseHomomorphism(h);
        pre.makeAllRulesExplicit();
//        System.out.println(pre);

        assertEquals(gold, pre);
    }

    @Test
    public void testViterbi() {
        BottomUpAutomaton auto = parse("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        Tree best = auto.viterbi();
        assertEquals(best.toString(), TermParser.parse("f(a,a)").toTree().toString());
    }

    @Test
    public void testInside() {
        BottomUpAutomaton auto = parse("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        Map inside = auto.inside();
        assertEquals(7.0, inside.get("q"), 0.001);
        assertEquals(2.0, inside.get("q1"), 0.001);
    }

    @Test
    public void testOutside() {
        BottomUpAutomaton auto = parse("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        Map inside = auto.inside();
        Map outside = auto.outside(inside);
        assertEquals(1.0, outside.get("q"), 0.001);
        assertEquals(5.5, outside.get("q1"), 0.001);
    }
    
    @Test
    public void testLanguage() {
        BottomUpAutomaton auto = parse("a -> q1 [2]\n b -> q2 [1]\n f(q1,q1) -> q! [1]\n f(q1,q2) -> q! [1.5]");
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet([pt("f(a,a)"), pt("f(a,b)")]*.toString());
        assertEquals(gold, lang);
    }
    
    @Test
    public void testLanguage2() {
        BottomUpAutomaton auto = parse("a -> q1\n b -> q1\n c -> q2\n d -> q2\n f(q1,q2) -> q!\n g(q1,q2) -> q!");
        Set lang = new HashSet(auto.language()*.toString());
        Set gold = new HashSet(["f(a,c)", "f(a,d)", "f(b,c)", "f(b,d)", "g(a,c)", "g(a,d)", "g(b,c)", "g(b,d)"].collect {pt(it).toString()});
        assertEquals(gold, lang)
    }
    
    private static Tree pt(String s) {
        TermParser.parse(s).toTree()
    }

    private static Pair p(Object a, Object b) {
        return new Pair(a,b);
    }

    private static BottomUpAutomaton parse(String s) {
        return BottomUpAutomatonParser.parse(new StringReader(s));
    }

    private static Rule r(parent, label, children) {
        return new Rule(parent, label, children);
    }
}

