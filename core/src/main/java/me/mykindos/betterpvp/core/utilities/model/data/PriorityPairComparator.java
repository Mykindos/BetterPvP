package me.mykindos.betterpvp.core.utilities.model.data;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;

public class PriorityPairComparator implements Comparator<Pair<PriorityData, ?>> {
    @Override
    public int compare(Pair<PriorityData, ?> o1, Pair<PriorityData, ?> o2) {
        int compare = Integer.compare(o1.getKey().getPriority(), o2.getKey().getPriority());
        //if compare is 0, we need to compare the instance, get the highest number
        if (compare == 0) {
            return -1 * Long.compare(o1.getKey().getInstance(), o2.getKey().getInstance());
        }
        return compare;
    }
}
