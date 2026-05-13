package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerStartFishingEvent;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNode;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionNodeManager;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;
import java.util.Optional;

@Singleton
@BPvPListener
@PluginAdapter("Progression")
public class FishingListener implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProfessionNodeManager progressionSkillManager;

    @Inject
    public FishingListener(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProfessionNodeManager.class);
    }

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();
        if (clanManager.isInSafeZone(player)) {
            denyFishing(event, player, "You cannot fish in a <aqua>Safe Zone</aqua>.");
            return;
        }

        if (!hasBaseFishing(player)) {
            Optional<Clan> playerClan = clanManager.getClanByPlayer(player);
            Optional<Clan> hookClan = clanManager.getClanByLocation(event.getHook().getLocation());
            Optional<Clan> standClan = clanManager.getClanByLocation(player.getLocation());

            // Standing or hooking inside the player's own clan territory requires Base Fishing.
            if (playerClan.isPresent()) {
                Clan own = playerClan.get();
                if (standClan.map(own::equals).orElse(false) || hookClan.map(own::equals).orElse(false)) {
                    denyFishing(event, player, "You must unlock <green>Base Fishing</green> to fish in your own territory");
                    return;
                }
            }

            // Hook landing in any non-Fields clan territory is gated.
            if (hookClan.isPresent() && !hookClan.get().getName().equalsIgnoreCase("Fields")) {
                denyFishing(event, player, "You can only fish at <yellow>Fields</yellow> or in the <yellow>Wilderness</yellow>.");
            }
        }
    }

    private boolean hasBaseFishing(Player player) {
        Optional<ProfessionNode> skill = progressionSkillManager.getSkill("base_fishing");
        if (skill.isEmpty()) {
            return true; // skill node missing — fail-open so fishing isn't blocked
        }
        return professionProfileManager.getObject(player.getUniqueId().toString())
                .map(profile -> profile.getProfessionDataMap().get("Fishing"))
                .map(data -> data.getBuild().getSkillLevel(skill.get()) >= 1)
                .orElse(false);
    }

    // PlayerStartFishingEvent isn't Cancellable; removing the hook is the only way to short-circuit.
    private void denyFishing(PlayerStartFishingEvent event, Player player, String message) {
        event.getHook().remove();
        UtilMessage.simpleMessage(player, "Fishing", message);
    }
}
