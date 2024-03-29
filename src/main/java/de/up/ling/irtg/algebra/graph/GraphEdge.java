/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.algebra.graph;

import java.util.function.Function;


/**
 *
 * @author koller
 */
public class GraphEdge {
    private GraphNode source;
    private GraphNode target;
    private String label;

    public GraphEdge(GraphNode source, GraphNode target) {
        this.source = source;
        this.target = target;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public GraphNode getSource() {
        return source;
    }

    public GraphNode getTarget() {
        return target;
    }
    
    public String repr() {
        return source.getName() + " -" + (label == null ? "-" : label) + "-> " + (target.getName());
    }
    
    public static final Function<GraphEdge,String> reprF =
        new Function<GraphEdge, String>() {
            public String apply(GraphEdge f) {
                return f.repr();
            }
        };
    
    public static final Function<GraphEdge,String> labelF =
        new Function<GraphEdge, String>() {
            public String apply(GraphEdge f) {
                return f.getLabel();
            }
        };
    
    @Override
    public String toString() {
        return label.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 97 * hash + (this.target != null ? this.target.hashCode() : 0);
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
        final GraphEdge other = (GraphEdge) obj;
        if (this.source != other.source && (this.source == null || !this.source.equals(other.source))) {
            return false;
        }
        if (this.target != other.target && (this.target == null || !this.target.equals(other.target))) {
            return false;
        }
        return true;
    }
    
    
}
