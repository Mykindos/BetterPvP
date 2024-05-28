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
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkill;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
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
public class BaseFishing implements Listener {

    private final ClanManager clanManager;
    private final ProfessionProfileManager professionProfileManager;
    private final ProgressionSkillManager progressionSkillManager;

    @Inject
    public BaseFishing(ClanManager clanManager) {
        this.clanManager = clanManager;
        final Progression progression = Objects.requireNonNull((Progression) Bukkit.getPluginManager().getPlugin("Progression"));
        this.professionProfileManager = progression.getInjector().getInstance(ProfessionProfileManager.class);
        this.progressionSkillManager = progression.getInjector().getInstance(ProgressionSkillManager.class);
    }

    @EventHandler
    public void onStartFishing(PlayerStartFishingEvent event) {
        Player player = event.getPlayer();

        Optional<ProgressionSkill> progressionSkillOptional = progressionSkillManager.getSkill("Base Fishing");
        if(progressionSkillOptional.isEmpty()) {
            return;
        }

        ProgressionSkill skill = progressionSkillOptional.get();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(skill);
                if (skillLevel >= 1) return;

                Optional<Clan> locationClanOptional = clanManager.getClanByLocation(event.getHook().getLocation());
                if(locationClanOptional.isPresent()) {
                    Clan locationClan = locationClanOptional.get();

                    Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player.getUniqueId());
                    if(playerClanOptional.isPresent()) {
                        Clan playerClan = playerClanOptional.get();
                        if(playerClan.equals(locationClan)) {
                            event.getHook().remove();
                            UtilMessage.simpleMessage(player, "Fishing", "You must unlock <green>Base Fishing</green> to fish in your own territory");
                            return;
                        }
                    }

                    if(!locationClan.getName().equalsIgnoreCase("Fields")) {
                        event.getHook().remove();
                        UtilMessage.simpleMessage(player, "Fishing", "You can only fish at <yellow>Fields</yellow> or in the <yellow>Wilderness</yellow>.");
                    }
                }
            }
        });
    }

}
