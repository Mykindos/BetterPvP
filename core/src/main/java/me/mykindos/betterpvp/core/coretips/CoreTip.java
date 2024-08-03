package me.mykindos.betterpvp.core.coretips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.tips.Tip;
import net.kyori.adventure.text.Component;

@Singleton
public abstract class CoreTip extends Tip {


    protected CoreTip(Core core, int defaultCategoryWeight, int defaultWeight, Component component) {
        super(core, defaultCategoryWeight, defaultWeight, component);
    }

    protected CoreTip(Core core, int defaultCategoryWeight, int defaultWeight) {
        super(core, defaultCategoryWeight, defaultWeight);
    }
}
