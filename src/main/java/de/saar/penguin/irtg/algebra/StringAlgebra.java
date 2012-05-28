/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.algebra;

import de.saar.basic.tree.Tree;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author koller
 */
public class StringAlgebra implements Algebra<List<String>> {
    public static final String CONCAT = "*";
    private static final Set<String> CONCAT_SET = new HashSet<String>();

    static {
        CONCAT_SET.add(CONCAT);
    }

    public List<String> evaluate(Tree<String> t) {
        List<String> children = t.getChildren(t.getRoot());

        if (children.isEmpty()) {
            List<String> ret = new ArrayList<String>();
            ret.add(t.getLabel(t.getRoot()));
            return ret;
        } else {
            List<String> childEval = new ArrayList<String>();

            // append yields of all children
            for (String child : children) {
                childEval.addAll(evaluate(t.subtree(child)));
            }

            return childEval;
        }
    }

    public TreeAutomaton decompose(List<String> words) {
        return new CkyAutomaton(words);
    }

    public List<String> parseString(String representation) {
        return Arrays.asList(representation.split("\\s+"));
    }

    private static class CkyAutomaton extends TreeAutomaton<Span> {
        private List<String> words;
        private Set<String> allLabels;

        public CkyAutomaton(List<String> words) {
            this.words = words;

            finalStates.add(new Span(0, words.size()));

            allLabels = new HashSet<String>();
            allLabels.add(CONCAT);
            allLabels.addAll(words);
        }

        @Override
        public Set<String> getAllLabels() {
            return allLabels;
        }

        @Override
        public Set<Span> getAllStates() {
            Set<Span> ret = new HashSet<Span>();

            for (int i = 0; i < words.size(); i++) {
                for (int k = i + 1; k <= words.size(); k++) {
                    ret.add(new Span(i, k));
                }
            }

            return ret;
        }

        @Override
        public Set<Rule<Span>> getRulesBottomUp(String label, List<Span> childStates) {
            if (useCachedRuleBottomUp(label, childStates)) {
                return getRulesBottomUpFromExplicit(label, childStates);
            } else {
                Set<Rule<Span>> ret = new HashSet<Rule<Span>>();

                if (label.equals(CONCAT)) {
                    if (childStates.size() != 2) {
                        return new HashSet<Rule<Span>>();
                    }

                    if (childStates.get(0).end != childStates.get(1).start) {
                        return new HashSet<Rule<Span>>();
                    }

                    Span span = new Span(childStates.get(0).start, childStates.get(1).end);
                    Rule<Span> rule = new Rule<Span>(span, label, childStates);
                    ret.add(rule);
                    storeRule(rule);

                    return ret;
                } else {
                    for (int i = 0; i < words.size(); i++) {
                        if (words.get(i).equals(label)) {
                            ret.add(new Rule<Span>(new Span(i, i + 1), label, new Span[]{}));
                        }
                    }

                    return ret;
                }
            }
        }

        @Override
        public Set<Rule<Span>> getRulesTopDown(String label, Span parentState) {
            if (!useCachedRuleTopDown(label, parentState)) {
                if (label.equals(CONCAT)) {
                    for (int i = parentState.start + 1; i < parentState.end; i++) {
                        List<Span> childStates = new ArrayList<Span>();
                        childStates.add(new Span(parentState.start, i));
                        childStates.add(new Span(i, parentState.end));
                        Rule<Span> rule = new Rule<Span>(parentState, label, childStates);
                        storeRule(rule);
                    }
                } else if ((parentState.length() == 1) && label.equals(words.get(parentState.start))) {
                    Rule<Span> rule = new Rule<Span>(parentState, label, new ArrayList<Span>());
                    storeRule(rule);
                }
            }

            return getRulesTopDownFromExplicit(label, parentState);
        }

        @Override
        public Set<String> getLabelsTopDown(Span parentState) {
            if (parentState.end == parentState.start + 1) {
                Set<String> ret = new HashSet<String>();
                ret.add(words.get(parentState.start));
                return ret;
            } else {
                return CONCAT_SET;
            }
        }

        @Override
        public boolean hasRuleWithPrefix(String label, List<Span> prefixOfChildren) {
            if( label.equals(CONCAT)) {
                switch(prefixOfChildren.size()) {
                    case 0:
                    case 1:
                        return true;
                        
                    case 2:
                        return prefixOfChildren.get(0).end == prefixOfChildren.get(1).start;
                        
                    default:
                        throw new RuntimeException("checking rule prefix for CONCAT with arity > 2");
                }
            } else {
                return words.contains(label);
            }
        }
        

        @Override
        public Set<Span> getFinalStates() {
            return finalStates;
        }
    }

    public static class Span implements Serializable {
        public int start, end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int length() {
            return end - start;
        }

        @Override
        public String toString() {
            return start + "-" + end;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Span other = (Span) obj;
            if (this.start != other.start) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 23 * hash + this.start;
            hash = 23 * hash + this.end;
            return hash;
        }
    }
}
