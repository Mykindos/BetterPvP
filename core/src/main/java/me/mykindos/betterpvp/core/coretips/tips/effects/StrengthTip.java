package me.mykindos.betterpvp.core.coretips.tips.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.coretips.CoreTip;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@Singleton
public class StrengthTip extends CoreTip {

    @Inject
    public StrengthTip(Core core) {
        super(core, 1, 1, Component.empty()
                .append(UtilMessage.deserialize(EffectTypes.STRENGTH.getGenericDescription())));
    }

    @Override
    public String getName() {
        return "strengthtip";
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
