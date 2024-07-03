package me.mykindos.betterpvp.core.utilities.model.data;

import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PriorityDataBlockingQueue<T> extends PriorityBlockingQueue<Pair<PriorityData, T>> {

    public PriorityDataBlockingQueue(int initialCapacity) {
        super(initialCapacity, new PriorityPairComparator());
    }

    /**
     * Put a new element with a priority, assigning it the correct instance
     * Time complexity: O(n)
     * @param priority the priority
     * @param element the element
     */
    public void put(int priority, T element) {
        AtomicLong instance = new AtomicLong();
        forEach(priorityDataTPair -> {
            PriorityData priorityData = priorityDataTPair.getKey();
            if (priorityData.getPriority() == priority) {
                if (priorityData.getInstance() >= instance.get()) {
                    instance.set(priorityData.getInstance() + 1);
                }
            }
        });
        PriorityData newPriorityData = new PriorityData(priority, instance.get());
        super.put(Pair.of(newPriorityData, element));
    }
}
