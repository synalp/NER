/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author rojasbar
 */
public class DoubleValueComparator /* implements Comparator*/ {

        Map base;

        public DoubleValueComparator(Map base) {
            this.base = base;
        }

        public int compare(Object a, Object b) {
            System.out.println("DoubleValueComparator val a:" +(Double) base.get(a)+" b: "+ (Double) base.get(b));
            
            Double vala=(Double) base.get(a);
            Double valb=(Double) base.get(b);
            
            if (vala.doubleValue() > valb.doubleValue()) {
                return 1;
            } else if (vala.doubleValue() == valb.doubleValue()) {
                return 0;
            } else {
                return -1;
            }
        }
        
        public static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
            SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
                new Comparator<Map.Entry<K,V>>() {
                    @Override public int compare(Map.Entry<K,V> e1, Map.Entry<K,V> e2) {
                        int res = e1.getValue().compareTo(e2.getValue());
                        if (e1.getValue().equals(e2.getValue())) {
                            return res; // Code will now handle equality properly
                        } else {
                            return res != 0 ? res : 1; // While still adding all entries
                        }
                    }
                }
            );
            sortedEntries.addAll(map.entrySet());
            return sortedEntries;
        }           
    }