package com.movieticket.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Unit 2: Java Collections and Data Handling - Generic Methods, Bounded Type Parameters,
 * Comparable, Comparator, ArrayList, and Iterator.
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * Generic method with bounded type parameter sorting elements by their natural Comparable order.
     */
    public static <T extends Comparable<T>> void sortNaturally(List<T> items) {
        if (items != null && items.size() > 1) {
            Collections.sort(items);
        }
    }

    /**
     * Generic method sorting elements using an explicit Comparator instance.
     */
    public static <T> void sortWithComparator(List<T> items, Comparator<T> comparator) {
        if (items != null && comparator != null && items.size() > 1) {
            Collections.sort(items, comparator);
        }
    }

    /**
     * Generic method extracting up to maxCount items using an explicit Iterator.
     */
    public static <T> List<T> takeFirst(List<T> source, int maxCount) {
        List<T> result = new ArrayList<T>();
        if (source == null || maxCount <= 0) {
            return result;
        }
        Iterator<T> iterator = source.iterator();
        int count = 0;
        while (iterator.hasNext() && count < maxCount) {
            result.add(iterator.next());
            count++;
        }
        return result;
    }
}

