package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@CustomLog
@Singleton
public class ClanEnergyFieldsTip extends ClanTip implements ISuggestCommand {

    private final ClanManager clanManager;

    @Inject
    public ClanEnergyFieldsTip(Clans clans, ClanManager clanManager) {
        super(clans, 1, 2);
        this.clanManager = clanManager;
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanenergyfields";
    }

    @Override
    public Component generateComponent() {
        return Component.text("You can earn energy at fields by mining amethyst clusters.", NamedTextColor.GRAY);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {

        return clan != null;
    }
}
