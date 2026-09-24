package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.CampConfig;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.clans.world.camp.structure.CampStructures;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Barracks upgrade: a member's first respawn in each cooldown comes with a short Regeneration. */
@BPvPListener
@Singleton
public class InfirmaryCot implements Listener {

    public static final String ID = "infirmary_cot";

    private final Clans clans;
    private final ClanManager clanManager;
    private final CampConfig config;
    private final CampUpgrades upgrades;
    private final EffectManager effects;
    private final Map<UUID, Long> lastTended = new HashMap<>();

    @Inject
    public InfirmaryCot(@NotNull Clans clans, @NotNull ClanManager clanManager, @NotNull CampConfig config,
                        @NotNull CampUpgrades upgrades, @NotNull EffectManager effects) {
        this.clans = clans;
        this.clanManager = clanManager;
        this.config = config;
        this.upgrades = upgrades;
        this.effects = effects;
        upgrades.declare(CampStructures.BARRACKS, ID, 2);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(@NotNull PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final boolean upgraded = clanManager.getClanByPlayer(player)
                .map(clan -> upgrades.has(Camps.keyFor(clan), CampStructures.BARRACKS, ID))
                .orElse(false);
        if (!upgraded) {
            return;
        }
        config.upgrade(CampStructures.BARRACKS, ID).ifPresent(numbers -> {
            if (!tend(player.getUniqueId(), System.currentTimeMillis(),
                    numbers.setting("cooldown-seconds", 3600) * 1000L)) {
                return;
            }
            final int level = numbers.setting("level", 1);
            final long length = numbers.setting("seconds", 8) * 1000L;
            Bukkit.getScheduler().runTask(clans, () -> {
                if (player.isOnline()) {
                    effects.addEffect(player, EffectTypes.REGENERATION, level, length);
                }
            });
        });
    }

    /** Whether {@code player} is tended at {@code now}, recording it if so. Once per {@code cooldown} milliseconds. */
    boolean tend(@NotNull UUID player, long now, long cooldown) {
        final Long last = lastTended.get(player);
        if (last != null && now - last < cooldown) {
            return false;
        }
        lastTended.put(player, now);
        return true;
    }
}
