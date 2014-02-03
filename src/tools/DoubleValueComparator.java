/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import java.util.Comparator;
import java.util.Map;

/**
 *
 * @author rojasbar
 */
public class DoubleValueComparator implements Comparator {

        Map base;

        public DoubleValueComparator(Map base) {
            this.base = base;
        }

        public int compare(Object a, Object b) {

            if ((Double) base.get(a) > (Double) base.get(b)) {
                return -1;
            } else if ((Double) base.get(a) == (Double) base.get(b)) {
                return 0;
            } else {
                return 1;
            }
        }
    }