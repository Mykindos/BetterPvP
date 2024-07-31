package me.mykindos.betterpvp.progression.tips;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.tips.Tip;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public abstract class ProgressionTip extends Tip {


    protected ProgressionTip(Progression progression, int defaultCategoryWeight, int defaultWeight, Component component) {
        super(progression, defaultCategoryWeight, defaultWeight, component);
    }

    protected ProgressionTip(Progression progression, int defaultCategoryWeight, int defaultWeight) {
        super(progression, defaultCategoryWeight, defaultWeight);
    }

    public boolean isValid(Player player, ProfessionProfile professionProfile) {
        return super.isValid(player);
    }
}
