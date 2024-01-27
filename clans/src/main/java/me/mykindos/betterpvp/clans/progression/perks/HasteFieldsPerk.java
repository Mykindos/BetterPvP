package me.mykindos.betterpvp.clans.progression.perks;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanChangeTerritoryEvent;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class HasteFieldsPerk implements Listener, ConfigAccessor, ProgressionPerk {

    private boolean enabled;
    private List<Integer> hasteLevels;
    private final ClanManager manager;
    private final Mining mining;

    private final Clans clans;

    @Inject
    public HasteFieldsPerk(final ClanManager clanManager, final ProgressionAdapter adapter, Clans clans) {
        this.manager = clanManager;
        this.mining = adapter.getProgression().getInjector().getInstance(Mining.class);
        this.clans = clans;
    }

    @Override
    public String getName() {
        return "Faster Ores";
    }

    @Override
    public List<String> getDescription(Player player, ProgressionData<?> data) {
        List<String> description = new ArrayList<>(List.of(
                "TODO"
        ));
        if (canUse(player, data)) {
            description.add("Can Use");
        }
        return description;
    }

    @Override
    public Class<? extends ProgressionTree>[] acceptedTrees() {
        return new Class[] {
                Mining.class
        };
    }

    @Override
    public boolean canUse(Player player, ProgressionData<?> data) {
        if (hasteLevels.size() <= 0) {
            return false;
        }
        return data.getLevel() >= hasteLevels.get(0);
    }

    @EventHandler
    public void onHoldPickaxe(PlayerItemHeldEvent event) {
        if (!enabled) return;
        if(!mining.isEnabled()) return;
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItem(event.getNewSlot());
        Material material = itemStack == null ? null : itemStack.getType();
        processHaste(player, material, manager.getClanByLocation(player.getLocation()).orElse(null));
    }

    @EventHandler
    public void onEnterTerritory(ClanChangeTerritoryEvent event) {
        if (!enabled) return;
        if(!mining.isEnabled()) return;
        Player player = event.getPlayer();
        processHaste(player, player.getInventory().getItemInMainHand().getType(), event.getToClan());
    }

    private void processHaste(Player player, @Nullable Material itemMaterial, @Nullable Clan clan) {
        if (clan == null) {
            removeHaste(player);
            return;
        }
        if (itemMaterial == null) {
            removeHaste(player);
            return;
        }
        mining.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
            if (hasPerk) {
                if (!UtilItem.isPickaxe(itemMaterial)) {
                    removeHaste(player);
                    return;
                }
                if (!(manager.isFields(clan) || manager.isLake(clan))) {
                    removeHaste(player);
                    return;
                }
                mining.getLevel(player).whenComplete((level, throwable1) -> {
                    level = 500;
                    for (int i = hasteLevels.size() - 1; i >= 0; i--) {
                        if (level >= hasteLevels.get(i)) {
                            addHaste(player, i);
                            break;
                        }
                    }
                }).exceptionally(throwable1 -> {
                    log.error("Failed to check if player " + player.getName() + " has a level ", throwable);
                    return null;
                });
            }
        }).exceptionally(throwable -> {
            log.error("Failed to check if player " + player.getName() + " has perk " + getName(), throwable);
            return null;
        });
    }

    private void removeHaste(Player player) {
        if (!player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) return;
        UtilServer.runTask(clans, () -> player.removePotionEffect(PotionEffectType.FAST_DIGGING));
        UtilMessage.message(player, "Mining", UtilMessage.deserialize("Your mining vigor has diminished"));
    }

    private void addHaste(Player player, int level) {
        if (player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) return;
        UtilServer.runTask(clans, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, -1, level)));
        UtilMessage.message(player, "Mining", UtilMessage.deserialize("Your mining vigor has increased"));
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.hasteLevels = config.getOrSaveIntegerList("mining.haste-fields-perk.hasteLevels", List.of(200, 400, 600, 800, 1000));
        this.enabled = config.getOrSaveBoolean("mining.haste-fields-perk.enabled", true);
    }
}
