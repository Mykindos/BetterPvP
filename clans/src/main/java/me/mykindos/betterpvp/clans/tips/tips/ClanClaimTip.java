package me.mykindos.betterpvp.clans.tips.tips;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.tips.ClanTip;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.tips.types.ISuggestCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Slf4j
@Singleton
public class ClanClaimTip extends ClanTip implements ISuggestCommand {

    @Inject
    @Config(path = "clans.claims.additional", defaultValue = "3")
    private int additionalClaims;

    @Inject
    public ClanClaimTip(Clans clans) {
        super(clans, 1, 2);
        setComponent(generateComponent());
    }

    @Override
    public String getName() {
        return "clanclaim";
    }

    @Override
    public Component generateComponent() {
        Component suggestComponent = suggestCommand("/c claim", "/c claim");
        return Component.text("You can claim territory by running ", NamedTextColor.GRAY).append(suggestComponent);
    }

    @Override
    public boolean isValid(Player player, Clan clan) {
        if (clan == null) {
            return false;
        }
        return clan.getAdminsAsPlayers().contains(player) && (clan.getTerritory().size() < (clan.getMembers().size() + additionalClaims));
    }
}
