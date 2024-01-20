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
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.model.ProgressionPerk;
import me.mykindos.betterpvp.progression.model.ProgressionTree;
import me.mykindos.betterpvp.progression.model.stats.ProgressionData;
import me.mykindos.betterpvp.progression.tree.mining.Mining;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

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
    public void onEnterTerritory(ClanChangeTerritoryEvent event) {
        if (!enabled) return;
        if(!mining.isEnabled()) return;
        Player player = event.getPlayer();
        //no change in territory we care about
        if (isLakeOrField(event.getFromClan()) == isLakeOrField(event.getToClan())) {
            return;
        }
        if (isLakeOrField(event.getFromClan()) && !isLakeOrField(event.getToClan())) {
            player.removePotionEffect(PotionEffectType.FAST_DIGGING);
            return;
        }

        mining.hasPerk(player, getClass()).whenComplete((hasPerk, throwable) -> {
        if (hasPerk) {
            mining.getLevel(player).whenComplete((level, throwable1) -> {
                for (int i = hasteLevels.size() - 1; i >= 0; i--) {
                    if (level >= hasteLevels.get(i)) {
                        int I = i;
                        UtilServer.runTask(clans, () -> player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, -1, I)));
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

    private boolean isLakeOrField(Clan clan) {
        return manager.isFields(clan) || manager.isLake(clan);
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.hasteLevels = config.getOrSaveObject("mining.haste-fields-perk.hasteLevels", List.of(200, 400, 600, 800, 1000), List.class);
        this.enabled = config.getOrSaveBoolean("mining.haste-fields-perk.enabled", true);
    }
}
