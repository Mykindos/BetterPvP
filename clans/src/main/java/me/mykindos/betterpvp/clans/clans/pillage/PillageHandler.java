package me.mykindos.betterpvp.clans.clans.pillage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
public class PillageHandler {

    @Getter
    private final List<Pillage> activePillages = new ArrayList<>();

    private final Clans clans;

    @Inject
    public PillageHandler(Clans clans) {
        this.clans = clans;
    }

    public boolean isPillaging(IClan pillager, IClan pillaged) {
        return activePillages.stream().anyMatch(pillage -> pillage.getPillager().equals(pillager)
                && pillage.getPillaged().equals(pillaged));
    }

    public boolean isBeingPillaged(IClan clan) {
        return activePillages.stream().anyMatch(pillage -> pillage.getPillaged().equals(clan));
    }

    public void startPillage(Pillage pillage) {
        activePillages.add(pillage);
        UtilMessage.simpleBroadcast("Clans", "<yellow>%s <gray>has pillaged <yellow>%s<gray>.",
                pillage.getPillager().getName(), pillage.getPillaged().getName());

        Clan clan = (Clan) pillage.getPillaged();
        clan.setTntRecoveryRunnable(null);

    }

    public void endPillage(Pillage pillage) {
        activePillages.remove(pillage);
        UtilMessage.simpleBroadcast("Clans", "The pillage on <yellow>%s <gray>has ended.",
                pillage.getPillaged().getName());

        updateClanScoreboard(pillage.getPillager());
        updateClanScoreboard(pillage.getPillaged());

    }

    private void updateClanScoreboard(IClan clan) {
        for(ClanMember member : clan.getMembers()) {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if(player != null) {
                UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)), 5);
            }
        }
    }

}
