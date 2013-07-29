/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 *
 * @author koller
 */
public class Homomorphism {
//    private static int gensymNext = 1;
    private static Pattern HOM_NON_QUOTING_PATTERN = Pattern.compile("([a-zA-Z*+_]([a-zA-Z0-9_*+-]*))|([?]([0-9]+))");
    private Map<Integer, Tree<HomomorphismSymbol>> mappings;
    private Signature srcSignature, tgtSignature;
    private boolean debug = false;

    public Homomorphism(Signature src, Signature tgt) {
        mappings = new HashMap<Integer, Tree<HomomorphismSymbol>>();
        srcSignature = src;
        tgtSignature = tgt;
    }
    
    /**
     * Adds a mapping to the homomorphism. The label is expected to exist
     * in the homomorphism's source signature. Its "mapping" (= the tree
     * h(label)) will be resolved according to the homomorphism's target
     * signature. If the symbols in "mapping" do not exist in the target
     * signature yet, they will be added, with the numbers of children in the
     * "mapping" tree as their arities.
     * 
     * @param label
     * @param mapping 
     */
    public void add(String label, Tree<String> mapping) {
        mappings.put(srcSignature.getIdForSymbol(label), HomomorphismSymbol.treeFromNames(mapping, tgtSignature));
    }

    /**
     * Adds a mapping to the homomorphism, using symbol IDs. The symbol
     * ID of "label" is relative to the homomorphism's source signature;
     * the symbol IDs of the constants in the "mapping" tree are relative
     * to the homomomorphism's target signature.
     * 
     * @param label
     * @param mapping 
     */
    public void add(int label, Tree<HomomorphismSymbol> mapping) {
        // symbols for target signature should already have been added
        // in constructing the "mapping" tree
        mappings.put(label, mapping);
    }

    /**
     * Returns the value h(label), using symbol IDs.
     * 
     * @param label
     * @return 
     */
    public Tree<HomomorphismSymbol> get(int label) {
        return mappings.get(label);
    }
    
    
    /**
     * Returns the value h(label). The label is resolved
     * according to the homomorphism's source signature, and
     * is expected to be known there. The labels in the returned
     * tree are elements of the homomorphism's target signature.
     * If necessary, the returned tree can be converted back to
     * a tree of HomomorphismSymbols using HomomorphismSymbo.treeFromNames
     * and the homomorphism's target signature.
     * 
     * @param label
     * @return 
     */
    public Tree<String> get(String label) {
        return HomomorphismSymbol.toStringTree(get(srcSignature.getIdForSymbol(label)), tgtSignature);
    }

    /*
     * Applies the homomorphism to the given tree. Returns the homomorphic image
     * of the tree under this homomorphism.
     * 
     */
    public Tree<Integer> applyRaw(Tree<Integer> tree) {
//        final Map<String, String> knownGensyms = new HashMap<String, String>();

        return tree.dfs(new TreeVisitor<Integer, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<Integer> node, List<Tree<Integer>> childrenValues) {
                Tree<Integer> ret = constructRaw(mappings.get(node.getLabel()), childrenValues);
                if (debug) {
                    System.err.println("\n" + node + ":");
                    System.err.println("  " + rhsAsString(mappings.get(node.getLabel())));
                    for (Tree<Integer> child : childrenValues) {
                        System.err.println("   + " + child);
                    }
                    System.err.println("  => " + ret);
                }
                return ret;
            }
        });
    }
    
    public Tree<String> apply(Tree<String> tree) {
        return getTargetSignature().resolve(applyRaw(getSourceSignature().addAllSymbols(tree)));
    }
    
    
    
    /**
     * Applies the homomorphism to a given input tree. Variables are substituted according to the "subtrees"
     * parameter: ?1, ?x1 etc. refer to the first entry in the list, and so on.
     * 
     * @param tree
     * @param subtrees
     * @param knownGensyms
     * @return 
     */
    private Tree<Integer> constructRaw(final Tree<HomomorphismSymbol> tree, final List<Tree<Integer>> subtrees) {
        final Tree<Integer> ret = tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<Integer>>() {
            @Override
            public Tree<Integer> combine(Tree<HomomorphismSymbol> node, List<Tree<Integer>> childrenValues) {
                HomomorphismSymbol label = node.getLabel();

                switch (label.getType()) {
                    case VARIABLE:
                        return subtrees.get(label.getValue());
                    case CONSTANT:
                        return Tree.create(label.getValue(), childrenValues);
                    default:
                        throw new RuntimeException("undefined homomorphism symbol type");
                }
            }
        });

        return ret;
    }
    
//    public Tree<String> construct(Tree<String> tree, List<Tree<)

    /*
    private String gensym(String gensymString, Map<String, String> knownGensyms) {
        int start = gensymString.indexOf("+");
        String prefix = gensymString.substring(0, start);
        String gensymKey = gensymString.substring(start);

        if (!knownGensyms.containsKey(gensymKey)) {
            knownGensyms.put(gensymKey, "_" + (gensymNext++));
        }

        return prefix + knownGensyms.get(gensymKey);
    }
    */


    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        
        for (Integer key : mappings.keySet()) {
            buf.append(srcSignature.resolveSymbolId(key) + " -> " + 
                    rhsAsString(mappings.get(key)) + "\n");
        }

        return buf.toString();
    }

    public String rhsAsString(Tree<HomomorphismSymbol> t) {
        Tree<String> resolvedTree = HomomorphismSymbol.toStringTree(t, tgtSignature);
//        
//        
//         resolvedTree = t.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<String>>() {
//            @Override
//            public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
//                switch(node.getLabel().getType()) {
//                    case CONSTANT:
//                        return Tree.create(tgtSignature.resolveSymbolId(node.getLabel().getValue()), childrenValues);
//                    case VARIABLE:
//                        return Tree.create("?" + (node.getLabel().getValue()+1));
//                    default:
//                        return Tree.create("***");
//                }
//            }
//        });
//        
        resolvedTree.setCachingPolicy(false);
        return resolvedTree.toString(HOM_NON_QUOTING_PATTERN);
    }

    public Signature getSourceSignature() {
        return srcSignature;
    }

    public Signature getTargetSignature() {
        return tgtSignature;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Homomorphism) {
            Homomorphism other = (Homomorphism) obj;
            
            int[] sourceRemap = srcSignature.remap(other.srcSignature);
            int[] targetRemap = tgtSignature.remap(other.tgtSignature);
            
            if( mappings.size() != other.mappings.size() ) {
                return false;
            }
            
            for( int srcSym : mappings.keySet() ) {
                if( sourceRemap[srcSym] == 0 ) {
                    return false;
                }
                
                Tree<HomomorphismSymbol> thisRhs = mappings.get(srcSym);
                Tree<HomomorphismSymbol> otherRhs = other.mappings.get(sourceRemap[srcSym]);
                
                if( ! equalRhsTrees(thisRhs, otherRhs, targetRemap)) {
                    return false;
                }
            }
            
            return true;
        }

        return false;
    }
    
    private boolean equalRhsTrees(Tree<HomomorphismSymbol> thisRhs, Tree<HomomorphismSymbol> otherRhs, int[] targetRemap) {
        if( thisRhs.getLabel().getType() != otherRhs.getLabel().getType() ) {
            return false;
        }
        
        switch(thisRhs.getLabel().getType()) {
            case CONSTANT:
                if( targetRemap[thisRhs.getLabel().getValue()] != otherRhs.getLabel().getValue() ) {
                    return false;
                }
                break;
                
            case VARIABLE:
                if( thisRhs.getLabel().getValue() != otherRhs.getLabel().getValue() ) {
                    return false;
                }
        }
        
        if( thisRhs.getChildren().size() != otherRhs.getChildren().size() ) {
            return false;
        }
        
        for( int i = 0; i < thisRhs.getChildren().size(); i++ ) {
            if( ! equalRhsTrees(thisRhs.getChildren().get(i), otherRhs.getChildren().get(i), targetRemap)) {
                return false;
            }
        }
        
        return true;
    }
    
    

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isNonDeleting() {
        for (Integer label : mappings.keySet()) {
            Tree<HomomorphismSymbol> rhs = mappings.get(label);
            Set<HomomorphismSymbol> variables = new HashSet<HomomorphismSymbol>();
            for (HomomorphismSymbol l : rhs.getLeafLabels()) {
                if (l.isVariable()) {
                    variables.add(l);
                }
            }

            if (variables.size() < srcSignature.getArity((int) label)) {
                return false;
            }
        }

        return true;
    }
}
