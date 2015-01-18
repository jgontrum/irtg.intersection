/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.automata.astar;

import java.io.Serializable;

/**
 * OutsideSummary from a span over a string. 
 * Contains only the number of words to the left of this object
 * and to the right.
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
public class SXOutside implements Serializable {
    private int wordsLeft;
    private int wordsRight;

    public SXOutside(int wordsLeft, int wordsRight) {
        this.wordsLeft = wordsLeft;
        this.wordsRight = wordsRight;
    }

    public int getWordsLeft() {
        return wordsLeft;
    }

    public void setWordsLeft(int wordsLeft) {
        this.wordsLeft = wordsLeft;
    }

    public int getWordsRight() {
        return wordsRight;
    }

    public void setWordsRight(int wordsRight) {
        this.wordsRight = wordsRight;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + this.wordsLeft;
        hash = 89 * hash + this.wordsRight;
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
        final SXOutside other = (SXOutside) obj;
        if (this.wordsLeft != other.wordsLeft) {
            return false;
        }
        if (this.wordsRight != other.wordsRight) {
            return false;
        }
        return true;
    }

   
    @Override
    public String toString() {
        return "SXOutside{" + "wordsLeft=" + wordsLeft + ", wordsRight=" + wordsRight + '}';
    }

    public long asLongEncoding(int state) {
        return ((long) state << 32) | (0xFFFFFFFFL & (wordsLeft << 16 | wordsRight));
    }
    
}
