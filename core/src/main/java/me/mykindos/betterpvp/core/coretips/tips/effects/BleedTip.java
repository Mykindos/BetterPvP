package me.mykindos.betterpvp.core.coretips.tips.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.coretips.CoreTip;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class BleedTip extends CoreTip {

    @Inject
    public BleedTip(Core core) {
        super(core, 1, 1, Component.empty()
                .append(EffectTypes.BLEED.getGenericDescriptionComponent()));
    }

    @Override
    public String getName() {
        return "bleedtip";
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
