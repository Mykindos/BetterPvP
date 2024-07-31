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
public class PoisonTip extends CoreTip {

    @Inject
    public PoisonTip(Core core) {
        super(core, 1, 1, Component.empty()
                .append(UtilMessage.deserialize(EffectTypes.POISON.getGenericDescription())));
    }

    @Override
    public String getName() {
        return "poisontip";
    }

    @Override
    public boolean isValid(Player player) {
        return true;
    }
}
