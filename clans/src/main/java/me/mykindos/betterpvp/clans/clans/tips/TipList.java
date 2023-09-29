package me.mykindos.betterpvp.clans.clans.tips;

import me.mykindos.betterpvp.core.utilities.model.WeighedList;

public class TipList extends WeighedList<Tip> {
    public void add(Tip tip){
        super.add(tip.getWeightCategory(), tip.getWeight(), tip);
    }
}
