package me.mykindos.betterpvp.core.coretips.tips.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.coretips.CoreTip;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class SpeedTip extends CoreTip {

    @Inject
    public SpeedTip(Core core) {
        super(core, 1, 1, Component.empty()
                .append(EffectTypes.SPEED.getGenericDescriptionComponent()));
    }

    @Override
    public String getName() {
        return "speedtip";
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
