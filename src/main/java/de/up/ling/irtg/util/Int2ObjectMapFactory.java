package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Serializable;

/**
 * A factory class for vending different sorts of Maps.
 *
 * @author Dan Klein (klein@cs.stanford.edu)
 * @author Kayur Patel (kdpatel@cs)
 */
public abstract class Int2ObjectMapFactory<V> implements Serializable {

  // allow people to write subclasses
  protected Int2ObjectMapFactory() {
  }

  private static final long serialVersionUID = 4529666940763477360L;

  @SuppressWarnings("unchecked")
  public static final Int2ObjectMapFactory HASH_MAP_FACTORY = new Int2ObjectHashMapFactory();


  /** Return a MapFactory that returns a HashMap.
   *  <i>Implementation note: This method uses the same trick as the methods
   *  like emptyMap() introduced in the Collections class in JDK1.5 where
   *  callers can call this method with apparent type safety because this
   *  method takes the hit for the cast.
   *
   *  @return A MapFactory that makes a HashMap.
   */
  @SuppressWarnings("unchecked")
  public static <V> Int2ObjectMapFactory<V> Int2ObjectHashMapFactory() {
    return HASH_MAP_FACTORY;
  }



  private static class Int2ObjectHashMapFactory<V> extends Int2ObjectMapFactory<V> {

    private static final long serialVersionUID = -9222344631596580863L;

    @Override
    public Int2ObjectMap<V> newMap() {
      return new Int2ObjectOpenHashMap<>();
    }

    @Override
    public Int2ObjectMap<V> newMap(int initCapacity) {
      return new Int2ObjectOpenHashMap<>(initCapacity);
    }

    @Override
    public IntSet newSet() {
      return new IntOpenHashSet();
    }


  } // end class HashMapFactory


  /**
   * Returns a new non-parameterized map of a particular sort.
   *
   * @return A new non-parameterized map of a particular sort
   */
  public abstract Int2ObjectMap<V> newMap();

  /**
   * Returns a new non-parameterized map of a particular sort with an initial capacity.
   *
   * @param initCapacity initial capacity of the map
   * @return A new non-parameterized map of a particular sort with an initial capacity
   */
  public abstract Int2ObjectMap<V> newMap(int initCapacity);

  /**
   * A set with the same <code>K</code> parameterization of the Maps.
   */
  public abstract IntSet newSet();

}
