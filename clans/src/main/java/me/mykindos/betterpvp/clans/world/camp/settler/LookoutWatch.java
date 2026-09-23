package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.settler.SettlerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A camp with a Lookout warns its online members when someone from outside the clan lands there. The same visitor is
 * reported once every few minutes, not every time they come and go.
 */
@BPvPListener
@Singleton
public class LookoutWatch implements Listener {

    private static final long QUIET_MILLIS = 5 * 60 * 1000L;

    private final Camps camps;
    private final ClanManager clanManager;
    private final SettlerService settlers;
    private final CampWideTraits campWide;
    private final Map<String, Long> reported = new ConcurrentHashMap<>();

    @Inject
    public LookoutWatch(@NotNull Camps camps, @NotNull ClanManager clanManager, @NotNull SettlerService settlers,
                        @NotNull CampWideTraits campWide) {
        this.camps = camps;
        this.clanManager = clanManager;
        this.settlers = settlers;
        this.campWide = campWide;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onArrive(@NotNull PlayerChangedWorldEvent event) {
        final Player visitor = event.getPlayer();
        final World world = visitor.getWorld();
        final OptionalLong clanId = camps.clanOf(world);
        if (clanId.isEmpty() || camps.isMember(visitor, world)) {
            return;
        }
        final boolean watched = settlers.roster(Camps.keyFor(clanId.getAsLong()))
                .map(roster -> campWide.any(roster, CampTraits.LOOKOUT))
                .orElse(false);
        if (!watched || !firstInAWhile(clanId.getAsLong(), visitor.getUniqueId())) {
            return;
        }

        final Component message = Translations.component("clans.camp.lookout.landed",
                Component.text(visitor.getName(), NamedTextColor.YELLOW)).color(NamedTextColor.RED);
        clanManager.getClanById(clanId.getAsLong()).ifPresent(clan -> clan.getMembers().forEach(member -> {
            final Player online = Bukkit.getPlayer(member.getUuid());
            if (online != null) {
                UtilMessage.message(online, Translations.component("clans.prefix.camp"), message);
            }
        }));
    }

    private boolean firstInAWhile(long clanId, @NotNull UUID visitor) {
        final long now = System.currentTimeMillis();
        reported.values().removeIf(at -> now - at > QUIET_MILLIS);
        return reported.putIfAbsent(clanId + ":" + visitor, now) == null;
    }
}
