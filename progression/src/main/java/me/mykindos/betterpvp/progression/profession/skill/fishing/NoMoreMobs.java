package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.profession.fishing.FishingHandler;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.loot.SwimmerType;
import me.mykindos.betterpvp.progression.profession.fishing.model.FishingLootType;
import me.mykindos.betterpvp.progression.profession.skill.ProfessionSkillNode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class NoMoreMobs extends ProfessionSkillNode implements Listener {

    @Inject
    private FishingHandler fishingHandler;

    @Inject
    public NoMoreMobs(String name) {
        super("No More Mobs");
    }

    @Override
    public String getName() {
        return "No More Mobs";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "You will no longer catch mobs while fishing."
        };
    }

    @EventHandler
    public void onStartFishing(PlayerCaughtFishEvent event) {
        Player player = event.getPlayer();

        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                if (event.getLoot().getType() instanceof SwimmerType) {
                    WeighedList<FishingLootType> lootTypesCopy = new WeighedList<>();

                    fishingHandler.getLootTypes().getMap().forEach((categoryWeight, multimap) -> {
                        multimap.entries().forEach(entry -> {
                            if (!(entry.getValue() instanceof SwimmerType)) {
                                lootTypesCopy.add(categoryWeight, entry.getKey(), entry.getValue());
                            }
                        });
                    });

                    // Get a random FishingLootType that is not a SwimmerType
                    FishingLootType newLoot = lootTypesCopy.random();

                    event.setLoot(newLoot.generateLoot());
                }
            }
        });
    }

    @Override
    public Material getIcon() {
        return Material.ZOMBIE_HEAD;
    }

}
