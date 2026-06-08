package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanPillageTip extends ClanTip {

    private final ClanManager clanManager;

    @Inject
    public ClanPillageTip(Clans clans, ClanManager clanManager) {
        super(clans, 2, 1);
        this.clanManager = clanManager;
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanpillage";
    }

    @Override
    public Component generateComponent() {
        Component percentComponent = Component.text("100%", NamedTextColor.YELLOW);
        Component enemyComponent = Translations.component("clans.tip.pillage.enemy").color(NamedTextColor.RED);
        return Translations.component("clans.tip.pillage", percentComponent, enemyComponent);
        // TODO make this more descriptive of the pillage process
    }

    @Override
    public  boolean isValid(Player player, Clan clan) {
        return clan != null && clanManager.isPillageEnabled();

    }

}
