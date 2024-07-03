package me.mykindos.betterpvp.progression.profession.skill.fishing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.event.PlayerCaughtFishEvent;
import me.mykindos.betterpvp.progression.profession.fishing.loot.TreasureType;
import me.mykindos.betterpvp.progression.profile.ProfessionProfileManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
@CustomLog
public class FeelingLucky extends FishingProgressionSkill implements Listener {

    private final ProfessionProfileManager professionProfileManager;

    @Inject
    protected FeelingLucky(Progression progression, ProfessionProfileManager professionProfileManager) {
        super(progression);
        this.professionProfileManager = professionProfileManager;
    }

    @Override
    public String getName() {
        return "Feeling Lucky";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "When catching treasure, you have a <green>" + UtilFormat.formatNumber(getChance(level), 2) + "%",
                "chance to double the rewards.",
                "This also applies to Legendaries."
        };
    }

    private double getChance(int level) {
        return 0.1 * Math.max(1, level);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCatchFish(PlayerCaughtFishEvent event) {
        if (!(event.getLoot().getType() instanceof TreasureType treasure)) return;
        Player player = event.getPlayer();
        professionProfileManager.getObject(player.getUniqueId().toString()).ifPresent(profile -> {
            var profession = profile.getProfessionDataMap().get("Fishing");
            if (profession != null) {
                int skillLevel = profession.getBuild().getSkillLevel(this);
                if (skillLevel <= 0) return;

                if (UtilMath.randDouble(0, 100) < getChance(skillLevel)) {
                    // We double the drop calculation, so just because you might receive 1 diamond originally, doesn't mean you'll necessarily receive 2 diamonds.
                    log.info("{} caught {} {} with Feeling Lucky.", player.getName(), treasure.getMaterial().name().toLowerCase())
                            .addClientContext(player).addLocationContext(event.getCaught().getLocation()).submit();

                    int count = UtilMath.RANDOM.ints(treasure.getMinAmount(), treasure.getMaxAmount() + 1)
                            .findFirst().orElse(treasure.getMinAmount());
                    ItemStack itemStack = new ItemStack(treasure.getMaterial(), count);
                    itemStack.editMeta(meta -> meta.setCustomModelData(treasure.getCustomModelData()));
                    player.getWorld().dropItem(event.getPlayer().getLocation(), itemStack);
                }

            }
        });
    }

    @Override
    public Material getIcon() {
        return Material.GOLD_INGOT;
    }

}
