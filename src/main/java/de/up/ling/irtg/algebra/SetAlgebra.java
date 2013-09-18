/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra;

import de.up.ling.irtg.automata.TreeAutomaton;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An algebra of sets. The elements of this algebra are relations (of arbitrary arity)
 * over some given universe. The algebra interprets the following
 * operations:
 *
 * <ul>
 * <li>project_i(R) projects its argument R (a k-place relation with k >= i) to
 * the set of i-th elements of this relation.</li>
 * <li>intersect_i(R,F) returns the set of all tuples of R (a k-place relation
 * with k >= i) whose i-th element is a member of F (a subset of the
 * universe).</li>
 * <li>uniq_a(A) returns the set A (a subset of the universe) itself if A = {a};
 * otherwise it returns the empty set.</li>
 * <li>T returns the universe.</li>
 * </ul>
 * 
 * Importantly, the decomposition automata for this algebra only implement
 * {@link TreeAutomaton#getRulesBottomUp(int, int[])}, not {@link TreeAutomaton#getRulesTopDown(int, int)}.
 * This means that you need to take care to only ever call methods on them
 * that look at rules bottom-up.<p>
 *
 * @author koller
 */
public class SetAlgebra extends EvaluatingAlgebra<Set<List<String>>> {
    private static final String PROJECT = "project_";
    private static final String INTERSECT = "intersect_";
    private static final String UNIQ = "uniq_";
    private static final String TOP = "T";
    private static final String[] SPECIAL_STRINGS = {PROJECT, INTERSECT, UNIQ};

    private Map<String, Set<List<String>>> atomicInterpretations;
    private Set<String> allIndividuals;
    private Set<List<String>> allIndividualsAsTuples;

    public SetAlgebra() {
        this.atomicInterpretations = null;

        allIndividuals = new HashSet<String>();
        allIndividualsAsTuples = new HashSet<List<String>>();
    }

    public SetAlgebra(Map<String, Set<List<String>>> atomicInterpretations) {
        this();

        setAtomicInterpretations(atomicInterpretations);
    }

    public final void setAtomicInterpretations(Map<String, Set<List<String>>> atomicInterpretations) {
        this.atomicInterpretations = atomicInterpretations;

        allIndividuals.clear();
        allIndividualsAsTuples.clear();

        for (Set<List<String>> sls : atomicInterpretations.values()) {
            for (List<String> ls : sls) {
                allIndividuals.addAll(ls);

                for (String x : ls) {
                    List<String> tuple = new ArrayList<String>();
                    tuple.add(x);
                    allIndividualsAsTuples.add(tuple);
                }
            }
        }
    }

    protected Set<List<String>> evaluate(String label, List<Set<List<String>>> childrenValues) {
        Set<List<String>> ret = null;

        if (label.startsWith(PROJECT)) {
            ret = project(childrenValues.get(0), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(INTERSECT)) {
            ret = intersect(childrenValues.get(0), childrenValues.get(1), Integer.parseInt(arg(label)) - 1);
        } else if (label.startsWith(UNIQ)) {
            ret = uniq(childrenValues.get(0), arg(label));
        } else if (label.equals(TOP)) {
            ret = allIndividualsAsTuples;
        } else {
            ret = atomicInterpretations.get(label);
        }

//        System.err.println("evaluate: " + label + childrenValues + " = " + ret);
        return ret;
    }

    private Set<List<String>> project(Set<List<String>> tupleSet, int pos) {
        Set<List<String>> ret = new HashSet<List<String>>();

        for (List<String> tuple : tupleSet) {
            List<String> l = new ArrayList<String>();

            if (pos < tuple.size()) {
                l.add(tuple.get(pos));
                ret.add(l);
            }
        }

        return ret;
    }

    private Set<List<String>> intersect(Set<List<String>> tupleSet, Set<List<String>> filterSet, int pos) {
        Set<String> filter = new HashSet<String>();
        Set<List<String>> ret = new HashSet<List<String>>();

        for (List<String> f : filterSet) {
            filter.add(f.get(0));
        }

        for (List<String> tuple : tupleSet) {
            if (pos < tuple.size()) {
                if (filter.contains(tuple.get(pos))) {
                    ret.add(tuple);
                }
            }
        }

        return ret;
    }

    private Set<List<String>> uniq(Set<List<String>> tupleSet, String value) {
        List<String> uniqArg = new ArrayList<String>();

        uniqArg.add(value);

        if (tupleSet.size() == 1 && tupleSet.iterator().next().equals(uniqArg)) {
            return tupleSet;
        } else {
            return new HashSet<List<String>>();
        }
    }

    private static String arg(String stringWithArg) {
        for (String s : SPECIAL_STRINGS) {
            if (stringWithArg.startsWith(s)) {
                return stringWithArg.substring(s.length());
            }
        }

        return null;
    }

    @Override
    protected boolean isValidValue(Set<List<String>> value) {
        return ! value.isEmpty();
    }

    @Override
    public Set<List<String>> parseString(String representation) throws ParserException {
        try {
            return SetParser.parse(new StringReader(representation));
        } catch (ParseException ex) {
            throw new ParserException(ex);
        }

    }
}
