package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanEnergyTip extends ClanTip {

    @Inject
    public ClanEnergyTip(Clans clans) {
        super(clans, 2, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanenergy";
    }

    @Override
    public Component generateComponent() {
        Component energyComponent = Translations.component("clans.tip.energy.highlight").color(NamedTextColor.LIGHT_PURPLE);
        return Translations.component("clans.tip.energy", energyComponent);
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null;
    }

}
