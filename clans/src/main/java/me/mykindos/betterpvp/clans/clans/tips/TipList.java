package me.mykindos.betterpvp.clans.clans.tips;

import me.mykindos.betterpvp.core.utilities.model.WeighedList;

public class TipList extends WeighedList<TipOld> {
    public void add(TipOld tipOld){
        super.add(tipOld.getWeightCategory(), tipOld.getWeight(), tipOld);
    }
}
