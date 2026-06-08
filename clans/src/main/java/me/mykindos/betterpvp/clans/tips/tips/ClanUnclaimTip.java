package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
public class ClanUnclaimTip extends ClanTip implements ISuggestCommand {

    @Inject
    public ClanUnclaimTip(Clans clans) {
        super(clans, 1, 1);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanunclaim";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/c unclaim", "/c unclaim");
        return Translations.component("clans.tip.unclaim", suggestComponent).colorIfAbsent(NamedTextColor.GRAY);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        return clan != null && clan.getAdminsAsPlayers().contains(player) && !clan.getTerritory().isEmpty();
    }
}
