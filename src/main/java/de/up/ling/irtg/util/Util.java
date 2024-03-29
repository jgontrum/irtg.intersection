/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import de.up.ling.tree.Tree;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 * @author koller
 */
public class Util {
    private static Random rnd = new Random();
    
    public static Tree<String> makeBinaryTree(String symbol, List<String> leaves) {
        return makeBinaryTree(symbol, leaves, 0);
    }

    private static Tree<String> makeBinaryTree(String symbol, List<String> leaves, int pos) {
        int remaining = leaves.size() - pos;

        if (remaining == 1) {
            return Tree.create(leaves.get(pos));
        } else {
            Tree<String> left = Tree.create(leaves.get(pos));
            Tree<String> right = makeBinaryTree(symbol, leaves, pos + 1);
            return Tree.create(symbol, new Tree[]{left, right});
        }
    }

    public static Tree<String> makeTreeWithArities(Tree<String> tree) {
        if (tree.getChildren().isEmpty()) {
            if (tree.getLabel().startsWith("?")) {
                return tree;
            } else {
                return Tree.create(tree.getLabel() + "_0");
            }
        } else {
            List<Tree<String>> mappedChildren = mapList(tree.getChildren(), Util::makeTreeWithArities);
            return Tree.create(tree.getLabel() + "_" + mappedChildren.size(), mappedChildren);
        }
    }

    public static <I, O> List<O> mapList(List<I> values, Function<I, O> fn) {
        return values.stream().map(fn).collect(Collectors.toList());
    }
    
    public static <I, O> Set<O> mapSet(Set<I> values, Function<I, O> fn) {
        return values.stream().map(fn).collect(Collectors.toSet());
    }
    
    public static <E, Up> Tree<Up> mapTree(Tree<E> tree, com.google.common.base.Function<E,Up> fn) {
        return tree.dfs((node, children) -> Tree.create(fn.apply(node.getLabel()), children));
    }
    
    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static String getFilenameExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        return extension;
    }

    public static String stripExtension(String filename) {
        Pattern p = Pattern.compile("(.*)\\.[^.]*");
        Matcher m = p.matcher(filename);

        if (m.matches()) {
            return m.group(1);
        } else {
            return filename;
        }
    }

    public static String formatTime(long timeInNs) {
        if (timeInNs < 1000) {
            return timeInNs + " ns";
        } else if (timeInNs < 1000000) {
            return timeInNs / 1000 + " \u03bcs";
        } else if (timeInNs < 1000000000) {
            return timeInNs / 1000000 + " ms";
        } else {
            StringBuffer buf = new StringBuffer();

            if (timeInNs > 60000000000L) {
                buf.append(timeInNs / 60000000000L + "m ");
            }

            timeInNs %= 60000000000L;

            buf.append(String.format("%d.%03ds", timeInNs / 1000000000, (timeInNs % 1000000000) / 1000000));
            return buf.toString();
        }
    }

    public static String formatTimeSince(long start) {
        return formatTime(System.nanoTime() - start);
    }
    
    public static interface BottomUpTreeVisitor<L,V> {
        public V combine(Tree<L> node, List<V> childValues);
    }
    
    public static <L,V> V dfs(Tree<L> tree, BottomUpTreeVisitor<L,V> visitor) {
        List<V> childValues = mapList(tree.getChildren(), u -> dfs(u, visitor));
        return visitor.combine(tree, childValues);
    }
    
    public static <V> void forEachNode(Tree<V> tree, Consumer<V> visitor) {
        Void x = tree.dfs((node, children) -> { visitor.accept(node.getLabel()); return null; });
    }
    
    public static <E,F> com.google.common.base.Function<E,F> gfun(Function<E,F> javafun) {
        return new com.google.common.base.Function<E,F>() {
            @Override
            public F apply(E x) {
                return javafun.apply(x);
            }
            
        };
    }
    
    public static <E> List<E> makeList(int n, Supplier<E> sup) {
        List<E> ret = new ArrayList<>();
        
        for( int i = 0; i < n; i++ ) {
            ret.add(sup.get());
        }
        
        return ret;
    }
    
    public static <E> List<E> makeList(int n, IntFunction<E> sup) {
       List<E> ret = new ArrayList<>();
        
        for( int i = 0; i < n; i++ ) {
            ret.add(sup.apply(i));
        }
        
        return ret; 
    }
    
    public static void printToFile(String filename, String content) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(filename);
            fw.write(content);
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static long parseNumberWithPrefix(String str) {
        int indexStartPos = 0;
        int ret = 0;
        boolean foundIndex = false;

        while (indexStartPos < str.length() && !isDigit(str.charAt(indexStartPos))) {
            indexStartPos++;
        }

        while (indexStartPos < str.length()) {
            char c = str.charAt(indexStartPos++);

            if (isDigit(c)) {
                foundIndex = true;
                ret = 10 * ret + (c - '0');
            }
        }

        if (foundIndex) {
            return ret;
        } else {
            return -1;
        }
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }
    
    public static double mult(DoubleStream stream) {
        MutableDouble ret = new MutableDouble(1);        
        stream.forEach(ret::multiplyBy);
        return ret.getValue();
    }
    
    public static int sampleMultinomial(int[] values, IntToDoubleFunction prob) {
        double totalProb = Arrays.stream(values).mapToDouble(prob::applyAsDouble).sum();
        return sampleMultinomial(values, prob, totalProb);
    }
    
    public static int sampleMultinomial(int[] values, IntToDoubleFunction prob, double totalProb) {
        double selectProb = rnd.nextDouble() * totalProb;
        double cumulativeProb = 0;
        
        for( int i = 0; i < values.length; i++ ) {
            cumulativeProb += prob.applyAsDouble(values[i]);
            if( cumulativeProb >= selectProb ) {
                return values[i];
            }
        }
        
        return -1;
    }
}
