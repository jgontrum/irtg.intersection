/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.hom;

import com.google.common.base.Function;
import de.up.ling.irtg.signature.Signature;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeVisitor;
import java.util.List;

/**
 *
 * @author koller
 */
public class HomomorphismSymbol {
    public enum Type {
        CONSTANT, VARIABLE
        // GENSYM
    };
    private Type type;
    private int value;

    private HomomorphismSymbol(int value, Type type) {
        this.type = type;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public static HomomorphismSymbol createVariable(String varname) {
        return new HomomorphismSymbol(getVariableIndex(varname), Type.VARIABLE);
    }

    // TODO - avoid using this
    public static HomomorphismSymbol createConstant(String name, Signature signature, int arity) {
        return new HomomorphismSymbol(signature.addSymbol(name, arity), Type.CONSTANT);
    }

    public static HomomorphismSymbol createConstant(int name, Signature signature) {
        return new HomomorphismSymbol(name, Type.CONSTANT);
    }


    private static HomomorphismSymbol createFromName(String name, Signature signature, int arity) {
        if (name.startsWith("?")) {
            return createVariable(name);
        } else {
            return createConstant(name, signature, arity);
        }
    }

    /**
     * Converts a tree of string labels into a tree of HomomorphismSymbols.
     * Nodes with the labels ?1, ?2, ... will be converted into variables.
     * Constants will be resolved to symbol IDs using the given signature.
     * Constants that are not known in the signature will be added to the
     * signature, with the number of children in the tree as the arity.
     * 
     * @param tree
     * @param signature
     * @return 
     */
    public static Tree<HomomorphismSymbol> treeFromNames(Tree<String> tree, final Signature signature) {
        return tree.dfs(new TreeVisitor<String, Void, Tree<HomomorphismSymbol>>() {
            @Override
            public Tree<HomomorphismSymbol> combine(Tree<String> node, List<Tree<HomomorphismSymbol>> childrenValues) {
                return Tree.create(createFromName(node.getLabel(), signature, childrenValues.size()), childrenValues);
            }
        });
    }
    
    /**
     * Converts a tree of HomomorphismSymbols into a tree of string labels.
     * Symbol IDs in constant labels are resolved according to the given signature.
     * The tree returned by this method can be converted back into a tree of
     * HomomorphismSymbols using treeFromNames.
     * 
     * @param tree
     * @param signature
     * @return 
     */
    public static Tree<String> toStringTree(Tree<HomomorphismSymbol> tree, final Signature signature) {
        return tree.dfs(new TreeVisitor<HomomorphismSymbol, Void, Tree<String>>() {
            @Override
            public Tree<String> combine(Tree<HomomorphismSymbol> node, List<Tree<String>> childrenValues) {
                switch(node.getLabel().getType()) {
                    case VARIABLE:
                        return Tree.create("?" + (node.getLabel().getValue()+1));
                    case CONSTANT:
                        return Tree.create(signature.resolveSymbolId(node.getLabel().getValue()), childrenValues);
                }
                
                return null;
            }
        });
    }

    public boolean isConstant() {
        return getType() == Type.CONSTANT;
    }

    public boolean isVariable() {
        return getType() == Type.VARIABLE;
    }

    @Deprecated
    public int getIndex() {
        if (type != Type.VARIABLE) {
            return -1;
        } else {
            return value;
        }
    }

    private static int getVariableIndex(String varname) {
        int indexStartPos = 0;
        int ret = 0;
        boolean foundIndex = false;

        while (indexStartPos < varname.length() && !isDigit(varname.charAt(indexStartPos))) {
            indexStartPos++;
        }

        while (indexStartPos < varname.length()) {
            char c = varname.charAt(indexStartPos++);

            if (isDigit(c)) {
                foundIndex = true;
                ret = 10 * ret + (c - '0');
            }
        }

        if (foundIndex) {
            return ret - 1;
        } else {
            return -1;
        }
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    @Override
    public String toString() {
        return (isVariable()?"?":"") + value;
    }
    
    private static class HomSymbolToInt implements Function<HomomorphismSymbol,Integer> {
        public Integer apply(HomomorphismSymbol f) {
            return f.getValue();
        }
    }
    private static Function<HomomorphismSymbol,Integer> HOM_SYMBOL_TO_INT = new HomSymbolToInt();
    
    public static Function<HomomorphismSymbol,Integer> getHomSymbolToIntFunction() {
        return HOM_SYMBOL_TO_INT;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 79 * hash + this.value;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HomomorphismSymbol other = (HomomorphismSymbol) obj;
        if (this.type != other.type) {
            return false;
        }
        if (this.value != other.value) {
            return false;
        }
        return true;
    }

    
    
}
