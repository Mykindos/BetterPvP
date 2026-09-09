package me.mykindos.betterpvp.clans.world.camp;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.site.Admission;
import me.mykindos.betterpvp.core.world.site.SiteInstanceDormantEvent;
import me.mykindos.betterpvp.core.world.site.SiteInstances;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import me.mykindos.betterpvp.core.world.site.SiteOwners;
import me.mykindos.betterpvp.core.world.site.SiteOwnership;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the site framework has to be told in order for a clan to have a camp, and nothing else.
 * <p>
 * A camp is an owned site whose owner is a clan id, so the framework works in numbers and never learns what a clan
 * is. What is registered here is what makes that number mean something: which clan a player belongs to, what their
 * camp is built from, and who may walk into one.
 */
@BPvPListener
@Singleton
@CustomLog
public class Camps implements Listener, SiteOwnership {

    /** The site id a camp is configured under. */
    public static final String SITE_ID = "camp";

    @Inject
    @Config(path = "clans.camp.skins", defaultValue = "templates/camps/")
    private String skinFolder;

    private final ClanManager clanManager;
    private final CampStore store;
    private final SiteInstances instances;

    @Inject
    public Camps(@NotNull ClanManager clanManager, @NotNull CampStore store, @NotNull SiteInstances instances,
                 @NotNull SiteOwners owners) {
        this.clanManager = clanManager;
        this.store = store;
        this.instances = instances;

        owners.register(SITE_ID, this);
        Admission.register("clan", (key, occupants, party) -> admits(key, party.getMembers()));
    }

    /** The camp belonging to a clan. */
    public static @NotNull SiteKey keyFor(long clanId) {
        return SiteKey.of(SITE_ID, clanId);
    }

    public static @NotNull SiteKey keyFor(@NotNull Clan clan) {
        return keyFor(clan.getId());
    }

    @Override
    public @NotNull OptionalLong ownerOf(@NotNull Player player) {
        return clanManager.getClanByPlayer(player)
                .map(clan -> OptionalLong.of(clan.getId()))
                .orElseGet(OptionalLong::empty);
    }

    @Override
    public @NotNull Optional<String> templateFor(@NotNull SiteKey key) {
        return store.cached(key.getOwnerId())
                .map(Camp::getSkin)
                .map(skin -> skinFolder + skin);
    }

    /**
     * Reads a joining player's camp long before they could sail to it, so the answer is already in hand when a world
     * is being opened on the main thread.
     */
    @EventHandler
    public void onJoin(@NotNull ClientJoinEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> store.load(clan.getId()));
    }

    /** A disbanded clan's camp goes with it, both the world it stood in and the record of what was in it. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDisband(@NotNull ClanDisbandEvent event) {
        final SiteKey key = keyFor(event.getClan());
        store.delete(key.getOwnerId());

        instances.forKey(key).forEach(instance -> instances.release(instance.getId(), true).exceptionally(ex -> {
            log.warn("Could not release the camp world for disbanded clan {}", key.getOwnerId(), ex).submit();
            return null;
        }));
    }

    /** Writes a camp down once its world closes, which is the last moment anything in it can have changed. */
    @EventHandler
    public void onDormant(@NotNull SiteInstanceDormantEvent event) {
        final SiteKey key = event.getInstance().getKey();
        if (key.getSiteId().equals(SITE_ID)) {
            store.save(key.getOwnerId());
        }
    }

    /**
     * Whether a whole party may enter a camp.
     * <p>
     * Allies count, because a crew sails as one party and is admitted or refused as one. Members only would mean a
     * captain could not bring an ally home, and would strand that ally at sea rather than merely turning them away.
     */
    private boolean admits(@NotNull SiteKey key, @NotNull Set<UUID> party) {
        final Optional<Clan> owner = clanManager.getClanById(key.getOwnerId());
        if (owner.isEmpty()) {
            return false;
        }

        final Set<UUID> welcome = new HashSet<>();
        owner.get().getMembers().forEach(member -> welcome.add(member.getUuid()));
        owner.get().getAlliances().forEach(alliance ->
                alliance.getClan().getMembers().forEach(member -> welcome.add(member.getUuid())));

        return welcome.containsAll(party);
    }
}
