/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.io.Serializable;

/**
 *
 * @author koller
 */
public interface MapFactory extends Serializable {
    public Int2ObjectMap createMap(int depth);
}
