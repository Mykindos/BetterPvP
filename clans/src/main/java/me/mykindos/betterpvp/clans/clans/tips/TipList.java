package me.mykindos.betterpvp.clans.clans.tips;

import me.mykindos.betterpvp.core.utilities.model.WeighedList;

public class TipList extends WeighedList<Tip> {
    @Override
    public void add(Tip tip){
        return super.add(tip.getWeightCategory(), tip.getWeight(), tip);
    }
}
