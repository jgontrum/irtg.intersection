/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata.condensed;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import de.saar.basic.Pair;
import de.up.ling.irtg.automata.IntTrie;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.signature.IdentitySignatureMapper;
import de.up.ling.irtg.signature.SignatureMapper;
import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.IntInt2IntMap;
import de.up.ling.irtg.util.Util;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class CondensedEarleyIntersectionAutomaton<LeftState, RightState> extends TreeAutomaton<Pair<LeftState, RightState>> {

    private final TreeAutomaton<LeftState> left;
    private final CondensedTreeAutomaton<RightState> right;
    boolean DEBUG = false;
    private final SignatureMapper leftToRightSignatureMapper;

    private final IntInt2IntMap stateMapping;  // right state -> left state -> output state
    // (index first by right state, then by left state because almost all right states
    // receive corresponding left states, but not vice versa. This keeps outer map very dense,
    // and makes it suitable for a fast ArrayMap)

    public CondensedEarleyIntersectionAutomaton(TreeAutomaton<LeftState> left, CondensedTreeAutomaton<RightState> right, SignatureMapper sigMapper) {
        super(left.getSignature());

        this.left = left;
        this.right = right;

        this.leftToRightSignatureMapper = sigMapper;

        stateMapping = new IntInt2IntMap();

        finalStates = null;
    }

    private class Item {
        int cachedHashCode;
        CondensedRule rightRule;
        int dot;
        IntTrie<Int2ObjectMap<Set<Rule>>> leftRulePrefix;

        public Item(CondensedRule rightRule) {
            this.rightRule = rightRule;
            dot = 0;
            leftRulePrefix = left.getExplicitRulesBottomUp();

            computeHashcode();
            
//            cachedHashCode = 7;
//            cachedHashCode = 73 * cachedHashCode + rightRule.hashCode();
//            cachedHashCode = 73 * cachedHashCode + dot;
//            cachedHashCode = 73 * cachedHashCode; // make room for addition of dot position
            // leftRulePrefix is same for all items at this point,
            // so no need to put it in hashcode
        }
        
        private void computeHashcode() {
            cachedHashCode = 7;
            cachedHashCode = 73 * cachedHashCode + rightRule.hashCode();
            cachedHashCode = 73 * cachedHashCode + dot;
            cachedHashCode = 73 * cachedHashCode; // make room for addition of dot position
            // leftRulePrefix is same for all items at this point,
            // so no need to put it in hashcode
            
            cachedHashCode += leftRulePrefix.hashCode();
        }

        public int getNextRightChild() {
            return rightRule.getChildren()[dot];
        }

        public boolean isComplete() {
            return dot >= rightRule.getArity();
        }

        public Item moveDot(int leftState) {
            IntTrie<Int2ObjectMap<Set<Rule>>> next = leftRulePrefix.getOneStep(leftState);

            if (next != null) {
                Item ret = new Item(rightRule);
                ret.leftRulePrefix = next;
                ret.dot = dot + 1;
                
                ret.computeHashcode();

//                ret.cachedHashCode = cachedHashCode + 73; // update "dot" part of hashcode to new 73*dot
//                ret.cachedHashCode += 11 * leftState;       // identify path within left rule trie

                return ret;
            } else {
                return null;
            }
        }

        public void foreachLeftRuleHere(IntSet rightLabelSet, Consumer<Rule> fn) {
            final Int2ObjectMap<Set<Rule>> leftRuleMap = leftRulePrefix.getValue();

            if (leftRuleMap != null) {
                FastutilUtils.forEach(rightLabelSet, rightLabel -> {
                    Set<Rule> leftRulesHere = leftRuleMap.get(leftToRightSignatureMapper.remapBackward(rightLabel));

                    if (leftRulesHere != null) {
                        for (Rule leftRule : leftRulesHere) {
                            fn.accept(leftRule);
                        }
                    }
                });
            }
        }

        public IntSet getNextLeftStates() {
            return leftRulePrefix.getOneStepKeys();
        }

        @Override
        public int hashCode() {
            return cachedHashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Item other = (Item) obj;

            if (rightRule != other.rightRule) { // object identity -- rule objects from same automaton
                return false;
            }

            if (this.dot != other.dot) {
                return false;
            }

            if (leftRulePrefix != other.leftRulePrefix) { // object identity -- rule trie from same automaton
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "<<" + rightRule.toString(right, dot) + " with next " + Util.mapSet(getNextLeftStates(), q -> left.getStateForId(q)) + ">>";
        }        
    }

    private class Chart {
        private SetMultimap<Integer, Item> waitingFor;
        private Int2ObjectMap<Set<Item>> completeItems; // right -> set(item) with this right parent

        public Chart() {
            waitingFor = HashMultimap.create();
//            completeItems = new HashSet<Item>();
            completeItems = new ArrayMap<Set<Item>>();
        }

        public Set<Item> getWaitingFor(int rightState, int leftState) {
            return waitingFor.get(rightState);
        }
        
        private boolean addComplete(Item item) {
            Set<Item> itemsHere = completeItems.get(item.rightRule.getParent());
            
            if( itemsHere == null ) {
                itemsHere = new HashSet<Item>();
                completeItems.put(item.rightRule.getParent(), itemsHere);
            }
            
//            System.err.println("* add/complete " + itemsHere.size());
            return itemsHere.add(item);
        }
        
        private boolean addIncomplete(Item item) {
            return waitingFor.put(item.getNextRightChild(), item);
        }

        // returns true if item was new
        public boolean add(Item item) {
            if (item.isComplete()) {
                return addComplete(item);
            } else {
                return addIncomplete(item);
            }
        }

        @Override
        public String toString() {
            StringBuffer ret = new StringBuffer();
            
            for( int qr : completeItems.keySet() ) {
                ret.append("\nComplete items for right parent " + right.getStateForId(qr) + ":\n");
                
                for( Item it : completeItems.get(qr)) {
                    ret.append("  ** " + it.toString() + "\n");
                }
            }
            
            // TODO - add incomplete
            
            return ret.toString();
        }
        
        
    }
    
    /*
    private class Chart {
        private SetMultimap<Integer, Item> waitingFor;
        private Set<Item> completeItems;

        public Chart() {
            waitingFor
                    = waitingFor = HashMultimap.create();
            completeItems = new HashSet<Item>();
        }

        public Set<Item> getWaitingFor(int rightState, int leftState) {
            return waitingFor.get(rightState);
        }
        
        private boolean addComplete(Item item) {
            return completeItems.add(item);
        }
        
        private boolean addIncomplete(Item item) {
            return waitingFor.put(item.getNextRightChild(), item);
        }

        // returns true if item was new
        public boolean add(Item item) {
            if (item.isComplete()) {
                return addComplete(item);
            } else {
                return addIncomplete(item);
            }
        }
    }
    */

    /*
     // index by right+left in waitingFor
     private class Chart {

     private Int2ObjectMap<Int2ObjectMap<Set<Item>>> waitingFor;  // right dot -> left continue state -> set(item)
     private Set<Item> completeItems; // TODO - split up into smaller parts too

     public Chart() {
     waitingFor = new ArrayMap<>();
     completeItems = new HashSet<Item>();
     }

     // returns null if nobody is waiting for these states
     public Set<Item> getWaitingFor(int rightState, int leftState) {
     Int2ObjectMap<Set<Item>> waitingForRight = waitingFor.get(rightState);

     if (waitingForRight == null) {
     return null;
     } else {
     return waitingForRight.get(leftState);
     }
     }

     // returns true if item was new
     public boolean add(Item item) {
     if (item.isComplete()) {
     return completeItems.add(item);
     } else {
     int nextRightChild = item.getNextRightChild();
     Int2ObjectMap<Set<Item>> _waitingForRight = waitingFor.get(nextRightChild);
     MutableBoolean ret = new MutableBoolean(false);

     if (_waitingForRight == null) {
     _waitingForRight = new Int2ObjectOpenHashMap<Set<Item>>();
     waitingFor.put(nextRightChild, _waitingForRight);
     }

     // stupid reference copy to make it final, so I can access it
     // from closure below
     final Int2ObjectMap<Set<Item>> waitingForRight = _waitingForRight;

     FastutilUtils.forEach(item.getNextLeftStates(), leftState -> {
     Set<Item> itemsHere = waitingForRight.get(leftState);

     if (itemsHere == null) {
     itemsHere = new HashSet<Item>();
     waitingForRight.put(leftState, itemsHere);
     }

     ret.or(itemsHere.add(item));
     });

     return ret.getValue();
     }
     }
     }
     */
    // Intersecting the two automatons using a CKY algorithm
    @Override
    public void makeAllRulesExplicit() {
        if (!isExplicit) {
            isExplicit = true;
            getStateInterner().setTrustingMode(true);

            Queue<Item> agenda = new LinkedList<Item>();
            Chart chart = new Chart();

            // initialize with rules for start states
            FastutilUtils.forEach(right.getFinalStates(), fin -> {
                for (CondensedRule rule : right.getCondensedRulesByParentState(fin)) {
                    Item it = new Item(rule);
                    agenda.offer(it);
                    chart.add(it);
                }
            });

            while (!agenda.isEmpty()) {
                Item it = agenda.remove();
                System.err.println("\n\n----\n\nremove item: " + it);
                System.err.println("chart right now:\n" + chart);

                if (it.isComplete()) {
                    it.foreachLeftRuleHere(it.rightRule.getLabels(right), leftRule -> {
                        Rule combined = combineRules(leftRule, it.rightRule);
                        storeRule(combined);

                        Set<Item> waiting = chart.getWaitingFor(it.rightRule.getParent(), leftRule.getParent());

                        if (waiting != null) {
                            for (Item waitingItem : waiting) {
                                Item newItem = waitingItem.moveDot(leftRule.getParent());

                                if (newItem != null) {
                                    if (chart.add(newItem)) { // true if new
                                        agenda.offer(newItem);
                                    }
                                }
                            }
                        }
                    });
                } else {
                    for (CondensedRule rule : right.getCondensedRulesByParentState(it.getNextRightChild())) {
                        Item newItem = new Item(rule);

                        if (chart.add(newItem)) { // true if new
                            agenda.offer(newItem);
                        }
                    }
                }
            }

            getStateInterner().setTrustingMode(false);
        }
    }

    private void addStateMapping(int leftState, int rightState, int combinedState) {
        stateMapping.put(rightState, leftState, combinedState);
    }

    private int getStateMapping(int leftState, int rightState) {
        return stateMapping.get(rightState, leftState);
    }

    Rule combineRules(Rule leftRule, CondensedRule rightRule) {
        int[] childStates = new int[leftRule.getArity()];

        for (int i = 0; i < leftRule.getArity(); i++) {
            childStates[i] = addStatePair(leftRule.getChildren()[i], rightRule.getChildren()[i]);
        }

        int parentState = addStatePair(leftRule.getParent(), rightRule.getParent());

        return createRule(parentState, leftRule.getLabel(), childStates, leftRule.getWeight() * rightRule.getWeight());
    }

    private int addStatePair(int leftState, int rightState) {
        int ret = getStateMapping(leftState, rightState);

        if (ret == 0) {
            ret = addState(new Pair(left.getStateForId(leftState), right.getStateForId(rightState)));
            addStateMapping(leftState, rightState, ret);
//            stateMapping.put(rightState, leftState, ret);
        }

        return ret;
    }

    // get all states for this automaton, that are the result of the combination of a state in the
    // leftStates set and one in the rightStates set
    private void collectStatePairs(IntSet leftStates, IntSet rightStates, IntSet pairStates) {
        leftStates.forEach((leftState) -> {
            rightStates.stream().map((rightState) -> getStateMapping(leftState, rightState))
                    .filter((state)
                            -> (state != 0)).forEach((state) -> {
                        pairStates.add(state);
                    });
        });
    }

    @Override
    public IntSet getFinalStates() {
        if (finalStates == null) {
            getAllStates(); // initialize data structure for addState

            finalStates = new IntOpenHashSet();
            collectStatePairs(left.getFinalStates(), right.getFinalStates(), finalStates);
        }

        return finalStates;
    }

    @Override
    public Iterable<Rule> getRulesBottomUp(int labelId, int[] childStates) {
        return getRulesBottomUpFromExplicit(labelId, childStates);
    }

    @Override
    public Iterable<Rule> getRulesTopDown(int labelId, int parentState) {
        return getRulesTopDownFromExplicit(labelId, parentState);
    }

    @Override
    public boolean isBottomUpDeterministic() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static void main(String[] args) throws Exception {
        GenericCondensedIntersectionAutomaton.main(args, true, (left, right) -> {
            CondensedEarleyIntersectionAutomaton inter = new CondensedEarleyIntersectionAutomaton(left, right, new IdentitySignatureMapper(left.getSignature()));
            inter.makeAllRulesExplicit();
            return inter;
        });
    }
}
