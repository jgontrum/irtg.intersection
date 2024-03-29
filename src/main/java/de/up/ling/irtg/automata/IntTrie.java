/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.automata;

import de.up.ling.irtg.util.ArrayMap;
import de.up.ling.irtg.util.FastutilUtils;
import de.up.ling.irtg.util.MapFactory;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author koller
 */
public class IntTrie<E> implements Serializable {
    private Int2ObjectMap<IntTrie<E>> nextStep;
    private E value;
    private final MapFactory factory;

    private IntTrie(int depth, MapFactory factory) {
        this.factory = factory;
        nextStep = (Int2ObjectMap) factory.createMap(depth);
        value = null;
    }

    public IntTrie(MapFactory factory) {
        this(0, factory);
    }

    public IntTrie() {
        this(alwaysHashMapFactory);
    }

    

    private static final MapFactory alwaysHashMapFactory = depth -> new Int2ObjectOpenHashMap<>();

    /**
     * Returns the previously known entry, or null if an entry for this key was
     * not known.
     *
     * @param key
     * @param value
     * @return
     */
    public E put(int[] key, E value) {
        E ret = put(0, key, value);

//        if( ret != null ) {
//            allValues.remove(ret);
//        }
//        
//        allValues.add(value);
        return ret;
    }

    private E put(int depth, int[] key, E value) {
        if (depth == key.length) {
            E ret = this.value;
            this.value = value;
            return ret;
        } else {
            IntTrie<E> next = nextStep.get(key[depth]);
            if (next == null) {
                next = new IntTrie<E>(depth + 1, factory);
                nextStep.put(key[depth], next);
            }

            return next.put(depth + 1, key, value);
        }
    }

    public E get(int[] key) {
        return get(0, key);
    }

    private E get(int index, int[] key) {
        if (index == key.length) {
            return value;
        } else {
            IntTrie<E> next = nextStep.get(key[index]);
            if (next == null) {
                return null;
            } else {
                return next.get(index + 1, key);
            }
        }
    }

/**
     * Moves one step to the right and returns the sub-trie there.
     * 
     * @param key
     * @return 
     */
    public IntTrie<E> getOneStep(int key) {
        return nextStep.get(key);
    }
    
    /**
     * Returns the set of keys for which the trie can take one step
     * to the right.
     * 
     * @return 
     */
    public IntSet getOneStepKeys() {
        return nextStep.keySet();
    }
    
    /**
     * Returns the value stored at this node of the trie.
     * 
     * @return 
     */
    public E getValue() {
        return value;
    }


    public void foreachValueForKeySets(List<IntSet> keySets, Consumer<E> fn) {
        foreachValueForKeySets(0, keySets, fn);
    }

    private void foreachValueForKeySets(final int depth, final List<IntSet> keySets, final Consumer<E> fn) {
        if (depth == keySets.size()) {
            if (value != null) {
                fn.accept(value);
            }
        } else {
            final IntSet keysHere = keySets.get(depth);

            if (keysHere != null) {
                int nextStepSize = nextStep.size();

                if (keysHere.size() < nextStepSize) {
                    FastutilUtils.forEach(keysHere, key -> {
                        IntTrie<E> next = nextStep.get(key);

                        if (next != null) {
                            next.foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    });
                } else {
                    FastutilUtils.forEach(nextStep.keySet(), key -> {
                        if (keysHere.contains(key)) {
                            nextStep.get(key).foreachValueForKeySets(depth + 1, keySets, fn);
                        }
                    });
                }
            }
        }
    }

    public void foreach(Consumer<E> fn) {
        if (value != null) {
            fn.accept(value);
        }

        nextStep.values().forEach(next -> {
            next.foreach(fn);
        });
    }

    public static interface EntryVisitor<E> {

        public void visit(IntList keys, E value);
    }

    public void foreachWithKeys(EntryVisitor<E> visitor) {
        IntList keys = new IntArrayList();
        foreach(keys, visitor);
    }

    private void foreach(IntList keys, EntryVisitor<E> visitor) {
        if (value != null) {
            visitor.visit(keys, value);
        }

        FastutilUtils.foreachFastEntry(nextStep, (key, value) -> {
            int size = keys.size();
            keys.add(key);
            value.foreach(keys, visitor);
            keys.remove(size);
        });

//        for (int next : nextStep.keySet()) {
//            int size = keys.size();
//            keys.add(next);
//            nextStep.get(next).foreach(keys, visitor);
//            keys.remove(size);
//        }
    }

    public Collection<E> getValues() {
        final List<E> allValues = new ArrayList<E>();

        foreachWithKeys((keys, value) -> {
            allValues.add(value);
        });

        return allValues;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();

        foreachWithKeys((keys, value) -> {
            buf.append(keys + " -> " + value + "\n");
        });

        return buf.toString();
    }

    public void printStatistics() {
        Int2IntMap totalKeysPerDepth = new Int2IntOpenHashMap();
        Int2IntMap totalNodesPerDepth = new Int2IntOpenHashMap();
        Int2IntMap maxKeysPerDepth = new Int2IntOpenHashMap();
        collectStatistics(0, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth);

        for (int depth : totalKeysPerDepth.keySet()) {
            System.err.println("depth " + depth + ": " + totalNodesPerDepth.get(depth) + " nodes");
            System.err.println("max keys: " + maxKeysPerDepth.get(depth));
            System.err.println("avg keys: " + ((double) totalKeysPerDepth.get(depth)) / totalNodesPerDepth.get(depth) + "\n");
        }
        
        if( nextStep instanceof ArrayMap ) {
            System.err.println("uses ArrayMap with density " + ((ArrayMap) nextStep).getStatistics());
        }
    }

    private void collectStatistics(int depth, Int2IntMap totalKeysPerDepth, Int2IntMap totalNodesPerDepth, Int2IntMap maxKeysPerDepth) {
        int x = totalKeysPerDepth.get(depth);
        x += nextStep.keySet().size();
        totalKeysPerDepth.put(depth, x);

        x = totalNodesPerDepth.get(depth);
        x++;
        totalNodesPerDepth.put(depth, x);

        x = maxKeysPerDepth.get(depth);
        if (nextStep.keySet().size() > x) {
            maxKeysPerDepth.put(depth, nextStep.keySet().size());
        }

        for (int key : nextStep.keySet()) {
            nextStep.get(key).collectStatistics(depth + 1, totalKeysPerDepth, totalNodesPerDepth, maxKeysPerDepth);
        }
    }
}
