package me.mykindos.betterpvp.core.scene.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.Action;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.scene.SceneObjectFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class HumanNPC extends NPC {

    private final HumanNMS handle;

    /**
     * Players whose client currently has this NPC spawned. The NMS entity is not in the
     * world, so no entity tracker maintains this for us — {@link HumanNpcVisibilityListener}
     * uses it to re-send the NPC when a player re-enters range, changes world, or respawns.
     */
    private final Set<UUID> viewers = ConcurrentHashMap.newKeySet();

    @Setter
    private PlayerListVisibility playerListVisibility = PlayerListVisibility.INVISIBLE;

    public HumanNPC(HumanNMS handle, SceneObjectFactory factory) {
        super(factory);
        this.handle = handle;
        // The packet body exists independently of the chunk lifecycle (a world-less NMS entity shown per-player by
        // HumanNpcVisibilityListener), so bind it without activating. The NPC is chunk-managed only so its decorations
        // (e.g. nameplates) materialize/dematerialize with the anchor chunk instead of leaking into unloaded chunks.
        bindEntity(handle.getBukkitEntity());
        configureMaterialization(handle.getLocation(), null);
    }

    /**
     * Set the custom name of the NPC.
     *
     * @param component the custom name; {@code null} to fall back to the profile name
     */
    public void customName(@Nullable TextComponent component) {
        handle.getBukkitEntity().customName(component);
    }

    /**
     * Returns the entity as a {@link HumanEntity} for callers that need the narrower type.
     */
    @Override
    public HumanEntity getEntity() {
        return handle.getBukkitEntity();
    }

    /**
     * Shows this NPC to every online player unconditionally. Prefer letting
     * {@link HumanNpcVisibilityListener} drive visibility — it only shows the NPC to
     * players in range, and is the thing that re-shows it after the client discards it.
     */
    public void show() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            showTo(viewer);
        }
    }

    /**
     * Sends this NPC's tab-list profile entry to a single viewer. {@code listed} is driven
     * by {@link PlayerListVisibility}: an {@code INVISIBLE} NPC is still added (the client
     * needs the profile to render the skin) but is kept out of the tab list.
     */
    public void showTo(Player viewer) {
        final HumanEntity entity = getEntity();
        final UserProfile profile = new UserProfile(entity.getUniqueId(), entity.getName());
        if (handle.getSkinValue() != null && !handle.getSkinValue().isBlank()) {
            profile.getTextureProperties().add(new TextureProperty("textures", handle.getSkinValue(), handle.getSkinSignature()));
        }

        final boolean listed = playerListVisibility != PlayerListVisibility.INVISIBLE;
        final Component displayName = switch (playerListVisibility) {
            case CUSTOM_NAME_VISIBLE -> entity.customName() != null ? entity.customName() : Component.text(entity.getName());
            case PROFILE_NAME_VISIBLE -> Component.text(entity.getName());
            case INVISIBLE -> null;
        };

        final WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                profile, listed, 0, GameMode.SURVIVAL, displayName, null);
        final WrapperPlayServerPlayerInfoUpdate infoPacket = new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(Action.ADD_PLAYER, Action.UPDATE_LISTED, Action.UPDATE_DISPLAY_NAME), info);
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(infoPacket);

        // Player entities always render their profile name above the head; the only way to hide
        // it is a scoreboard team with name-tag visibility NEVER that contains the profile name.
        final WrapperPlayServerTeams.ScoreBoardTeamInfo teamInfo = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                Component.empty(), Component.empty(), Component.empty(),
                WrapperPlayServerTeams.NameTagVisibility.NEVER,
                WrapperPlayServerTeams.CollisionRule.NEVER,
                NamedTextColor.WHITE, WrapperPlayServerTeams.OptionData.NONE);
        final WrapperPlayServerTeams teamPacket = new WrapperPlayServerTeams(
                "npc" + getEntity().getEntityId(), WrapperPlayServerTeams.TeamMode.CREATE, Optional.of(teamInfo), List.of(entity.getName()));
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(teamPacket);

        // The entity is not in the world, so no tracker spawns it — send the spawn packet
        // ourselves. The profile entry above must arrive first for the client to render the skin.
        final Location loc = handle.getLocation();
        final WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entity.getEntityId(), Optional.of(entity.getUniqueId()), EntityTypes.PLAYER,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(), loc.getYaw(), loc.getYaw(), 0, Optional.of(new Vector3d(0, 0, 0)));
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(spawnPacket);

        // Skin overlay layers (hat, jacket, sleeves, pants) are toggled by the player entity's
        // customisation byte, which for real players comes from their client settings. This
        // entity has no client, so the byte defaults to 0 (all layers hidden) — send it
        // explicitly with every layer enabled or the skin renders without its second layer.
        final WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata(
                entity.getEntityId(),
                List.of(new EntityData<>(16, EntityDataTypes.BYTE, (byte) 0x7F)));
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(metadataPacket);

        viewers.add(viewer.getUniqueId());
    }

    /**
     * Whether {@code player}'s client currently has this NPC spawned (i.e. {@link #showTo}
     * was sent and has not been invalidated by a {@link #hideTo} or {@link #markHidden}).
     */
    public boolean isShownTo(Player player) {
        return viewers.contains(player.getUniqueId());
    }

    /**
     * Marks this NPC as no longer spawned on {@code player}'s client <b>without</b> sending
     * any packets. Use when the client discarded the entity on its own (world change,
     * respawn) so the visibility tracker re-sends it on the next pass.
     */
    public void markHidden(Player player) {
        viewers.remove(player.getUniqueId());
    }

    /** Despawns the NPC client-side and drops its tab-list profile entry for a single viewer. */
    public void hideTo(Player viewer) {
        final WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(getEntity().getEntityId());
        final WrapperPlayServerPlayerInfoRemove removePacket = new WrapperPlayServerPlayerInfoRemove(List.of(getEntity().getUniqueId()));
        final WrapperPlayServerTeams teamPacket = new WrapperPlayServerTeams(
                "npc" + getEntity().getEntityId(), WrapperPlayServerTeams.TeamMode.REMOVE, Optional.empty(), List.of());
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(destroyPacket);
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(removePacket);
        PacketEvents.getAPI().getPlayerManager().getUser(viewer).sendPacket(teamPacket);
        viewers.remove(viewer.getUniqueId());
    }

    /** Despawns the NPC client-side and drops its tab-list profile entry for every online player. */
    public void hide() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            hideTo(viewer);
        }
        viewers.clear();
    }

    @Override
    public void remove() {
        hide();
        super.remove();
    }

}
