/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXInside{
    private int span;

    public SXInside(int span) {
        this.span = span;
    }

    public int getSpan() {
        return span;
    }

    public void setSpan(int span) {
        this.span = span;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.span;
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
        
        final SXInside other = (SXInside) obj;
        if (this.span != other.span) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SXInside{" + "span=" + span + '}';
    }
    
    
    
}
