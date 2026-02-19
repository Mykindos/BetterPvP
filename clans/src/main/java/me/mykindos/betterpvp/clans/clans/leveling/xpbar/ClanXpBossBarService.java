package me.mykindos.betterpvp.clans.clans.leveling.xpbar;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.leveling.ClanExperience;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.utilities.model.display.TimedDisplayObject;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarColor;
import me.mykindos.betterpvp.core.utilities.model.display.bossbar.BossBarData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import static me.mykindos.betterpvp.core.utilities.UtilMessage.miniMessage;

/**
 * Pushes a transient 1-second XP boss bar entry into each online clan member's
 * {@link me.mykindos.betterpvp.core.client.gamer.Gamer#getBossBarQueue()} whenever their clan gains XP.
 */
@Singleton
public class ClanXpBossBarService {

    private final ClientManager clientManager;

    @Inject
    public ClanXpBossBarService(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    /**
     * Called by {@link me.mykindos.betterpvp.clans.clans.listeners.ClanExperienceListener}
     * whenever XP is granted to a clan.
     */
    public void notifyXpGain(Player player, Clan clan, long amount, String reason) {
        long level = clan.getExperience().getLevel();
        double xpIn = ClanExperience.xpInCurrentLevel(level, clan.getExperience().getXp());
        double xpNeeded = ClanExperience.xpRequiredForNextLevel(level);
        float progress = (float) Math.min(1.0, xpIn / Math.max(1, xpNeeded));

        Component name = Component.empty()
                .append(Component.text("+", NamedTextColor.GOLD))
                .append(Component.text(String.format("%,d", amount), NamedTextColor.GOLD))
                .append(miniMessage.deserialize("<exp></exp>"))
                .appendSpace()
                .append(Component.text("Clan XP", NamedTextColor.GOLD));

        BossBarData data = new BossBarData(name, progress);

        showBar(player, data);
    }

    private void showBar(Player player, BossBarData data) {
        clientManager.search().online(player).getGamer()
                .getBossBarQueue()
                .add(250, BossBarColor.TRANSPARENT, new TimedDisplayObject<>(2.0, true, gamer -> data));
    }

}
