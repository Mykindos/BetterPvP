package me.mykindos.betterpvp.core.utilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UtilCollection {

    /**
     * Adds the elements of B if they are not in A after the elements of A
     * @param collectionA
     * @param collectionB
     * @return
     * @param <T> the param of the collection
     */
    public static <T> List<T> addUnique(Collection<T> collectionA, Collection<T> collectionB) {
        List<T> returnList = new ArrayList<>();
        returnList.addAll(collectionA);
        returnList.addAll(collectionB.stream().filter(element -> !collectionA.contains(element)).toList());
        return returnList;
    }
}
