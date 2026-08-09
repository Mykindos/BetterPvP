package me.mykindos.betterpvp.clans.clans.map.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRelationshipEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanTerritoryEvent;
import me.mykindos.betterpvp.clans.clans.map.ClanMapService;
import me.mykindos.betterpvp.clans.clans.map.data.MapSettings;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.component.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

@BPvPListener
@Singleton
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MapListener implements Listener {

    /** Pitch of the page turn at the closest zoom; each step out drops it by {@link #ZOOM_PITCH_STEP}. */
    private static final float ZOOM_PITCH_BASE = 1.5F;
    private static final float ZOOM_PITCH_STEP = 0.175F;

    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final ClanMapService clanMapService;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        clanMapService.removePlayerMapData(player);

        for (ItemStack value : player.getInventory().all(Material.FILLED_MAP).values()) {
            player.getInventory().remove(value);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(itemStack -> itemStack.getType() == Material.FILLED_MAP);
    }

    @EventHandler
    public void onSpawn(PlayerRespawnEvent event) {
        final Boolean keepInventory = event.getPlayer().getWorld().getGameRuleValue(GameRules.KEEP_INVENTORY);
        if (keepInventory == Boolean.FALSE) {
            final ItemStack mapItem = clanMapService.createMapItem();
            event.getPlayer().getInventory().setItem(8, itemFactory.convertItemStack(mapItem).orElse(mapItem));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftMap(PrepareItemCraftEvent event) {
        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && item.getType() == Material.MAP) {
                event.getInventory().setResult(new ItemStack(Material.AIR));
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDropMap(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.FILLED_MAP) {
            event.getItemDrop().remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanAlly(ClanRelationshipEvent event) {
        for (ClanMember member : event.getClan().getMembers()) {
            clanMapService.invalidate(member.getUuid());
        }
        for (ClanMember member : event.getTargetClan().getMembers()) {
            clanMapService.invalidate(member.getUuid());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClaim(ClanTerritoryEvent event) {
        if (event.isCancelled()) {
            return;
        }
        clanMapService.claimsChanged();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClanLeave(MemberLeaveClanEvent event) {
        if (event.isCancelled()) {
            return;
        }
        clanMapService.invalidate(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDisband(ClanDisbandEvent event) {
        if (event.isCancelled()) {
            return;
        }
        clanMapService.invalidateAll();
        clanMapService.claimsChanged();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinClan(MemberJoinClanEvent event) {
        if (event.isCancelled()) {
            return;
        }
        clanMapService.invalidate(event.getPlayer().getUniqueId());
    }

    /**
     * Zoom is instant: the frame for a new zoom level is copied out of a precomputed mip, so there is nothing to rate
     * limit. The only guard needed is against Bukkit reporting a single click as two interact events, which a
     * same-tick check handles without imposing any delay on the player.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.FILLED_MAP) {
            return;
        }

        final boolean out = event.getAction().name().contains("RIGHT");
        final boolean in = event.getAction().name().contains("LEFT");
        if (!out && !in) {
            return;
        }

        final MapSettings settings = clanMapService.getOrCreateMapSettings(player);
        if (!settings.acceptZoom(Bukkit.getCurrentTick())) {
            return;
        }

        final MapSettings.Scale current = settings.getScale();
        final int ordinal = current.ordinal() + (out ? 1 : -1);
        if (ordinal < 0 || ordinal >= MapSettings.Scale.values().length) {
            return;
        }

        final MapSettings.Scale scale = settings.setScale(MapSettings.Scale.values()[ordinal]);
        settings.setForceRedraw(true);

        new SoundEffect(Sound.ITEM_BOOK_PAGE_TURN, pitch(scale), 0.8F).play(player);

        final Client client = clientManager.search().online(player);
        client.getGamer().getActionBar().add(500, new TimedComponent(1.5, false, gamer -> createZoomBar(scale)));
    }

    /** Closer zooms turn the page higher, so the sound tracks the direction of travel. */
    private float pitch(MapSettings.Scale scale) {
        return ZOOM_PITCH_BASE - scale.getExponent() * ZOOM_PITCH_STEP;
    }

    private Component createZoomBar(MapSettings.Scale scale) {
        return Component.text("Zoom: ", NamedTextColor.WHITE)
                .append(Component.text(scale.getValue() + "x", NamedTextColor.GREEN));
    }
}
