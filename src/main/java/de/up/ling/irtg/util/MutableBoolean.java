/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

/**
 *
 * @author koller
 */
public class MutableBoolean {
    private boolean value;

    public MutableBoolean() {
        value = false;
    }

    public MutableBoolean(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }
    
    public void or(boolean value) {
        this.value = this.value || value;
    }
}
