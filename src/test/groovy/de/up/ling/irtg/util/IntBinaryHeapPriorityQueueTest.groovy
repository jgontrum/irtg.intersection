/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*

/**
 *
 * @author Johannes Gontrum <gontrum@uni-potsdam.de>
 */
class IntBinaryHeapPriorityQueueTest {
	@Test
        void simpleQueueTest() {
        IntBinaryHeapPriorityQueue testqueue = new IntBinaryHeapPriorityQueue();
        testqueue.relaxPriority(10, 0.1);
        testqueue.relaxPriority(9, 0.2);
        testqueue.relaxPriority(8, 0.3);
        testqueue.relaxPriority(7, 0.4);
        testqueue.relaxPriority(6, 0.5);
        testqueue.relaxPriority(5, 0.6);
        testqueue.relaxPriority(4, 0.7);
        testqueue.relaxPriority(3, 0.8);
        testqueue.relaxPriority(2, 0.9);
        testqueue.relaxPriority(1, 1.0);
        
        while (testqueue.hasNext()) {
            int i = testqueue.removeFirst();
            System.err.println(i);
        }
        }
}

