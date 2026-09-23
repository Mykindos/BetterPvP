package me.mykindos.betterpvp.core.world.construction.blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.StructureCatalogue;
import me.mykindos.betterpvp.core.world.construction.StructureType;
import me.mykindos.betterpvp.core.world.schematic.BlockTransform;
import me.mykindos.betterpvp.core.world.schematic.Schematic;
import me.mykindos.betterpvp.core.world.schematic.SchematicService;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreview;
import me.mykindos.betterpvp.core.world.schematic.ghost.GhostPreviews;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Holding a blueprint in the main hand shows a ghost of its structure on the block the player looks at, green where it
 * can be built and red, with the reason, where it cannot. Each press of sneak turns it a quarter, and right-click builds
 * it there, using the blueprint up.
 */
@BPvPListener
@Singleton
public class BlueprintSessions implements Listener {

    private static final int REACH = 24;

    private final StructureBlueprintItem blueprint;
    private final ItemFactory itemFactory;
    private final StructureCatalogue catalogue;
    private final SchematicService schematics;
    private final ConstructionService construction;
    private final GhostPreviews previews;

    private final Map<UUID, Session> sessions = new HashMap<>();

    @Inject
    public BlueprintSessions(@NotNull StructureBlueprintItem blueprint, @NotNull ItemFactory itemFactory,
                             @NotNull StructureCatalogue catalogue, @NotNull SchematicService schematics,
                             @NotNull ConstructionService construction, @NotNull GhostPreviews previews) {
        this.blueprint = blueprint;
        this.itemFactory = itemFactory;
        this.catalogue = catalogue;
        this.schematics = schematics;
        this.construction = construction;
        this.previews = previews;
    }

    /** A blueprint for {@code type}. */
    public @NotNull ItemStack blueprintFor(@NotNull StructureType type) {
        return itemFactory.create(blueprint).withComponent(new StructureBlueprintComponent(type.getId())).createItemStack();
    }

    /** Which structure {@code stack} is a blueprint for, if it is one. */
    public @NotNull Optional<StructureType> structureOf(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        return itemFactory.fromItemStack(stack)
                .filter(instance -> instance.getBaseItem() == blueprint)
                .flatMap(instance -> instance.getComponent(StructureBlueprintComponent.class))
                .flatMap(component -> catalogue.find(component.getStructure()));
    }

    @UpdateEvent(delay = 100)
    public void follow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final Optional<StructureType> held = structureOf(player.getInventory().getItemInMainHand());
            if (held.isEmpty()) {
                end(player);
                continue;
            }
            session(player, held.get()).ifPresent(session -> update(player, session));
        }
    }

    @EventHandler
    public void onSneak(@NotNull PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        final Session session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null) {
            session.quarterTurns = Math.floorMod(session.quarterTurns + 1, 4);
            update(event.getPlayer(), session);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(@NotNull PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        final Player player = event.getPlayer();
        final Optional<StructureType> held = structureOf(player.getInventory().getItemInMainHand());
        if (held.isEmpty()) {
            return;
        }
        event.setCancelled(true);

        final Session session = sessions.get(player.getUniqueId());
        final Location anchor = anchor(player);
        if (session == null || anchor == null) {
            player.sendActionBar(Translations.component("core.construction.blueprint.look_at_ground").color(NamedTextColor.RED));
            return;
        }

        final ConstructionResult result = construction.build(player, player.getWorld(), held.get(), anchor,
                session.quarterTurns);
        if (!result.isSuccess()) {
            if (result.getReason() != null) {
                UtilMessage.message(player, Translations.component("core.prefix.construction"), result.getReason());
            }
            return;
        }

        player.getInventory().getItemInMainHand().subtract();
        end(player);
        UtilMessage.message(player, Translations.component("core.prefix.construction"),
                Translations.component("core.construction.blueprint.started", held.get().getDisplayName()));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    private @NotNull Optional<Session> session(@NotNull Player player, @NotNull StructureType type) {
        final Session existing = sessions.get(player.getUniqueId());
        if (existing != null && existing.type.equals(type.getId())) {
            return Optional.of(existing);
        }

        final Optional<Schematic> schematic = schematics.load(type.stage(0).getSchematic());
        if (schematic.isEmpty()) {
            return Optional.empty();
        }
        final int facing = BlockTransform.quarterTurnsBetween(schematic.get().getAnchorYaw(), player.getLocation().getYaw());
        final Session session = new Session(type.getId(), previews.open(player, schematic.get()), facing);
        sessions.put(player.getUniqueId(), session);
        return Optional.of(session);
    }

    private void update(@NotNull Player player, @NotNull Session session) {
        final Optional<StructureType> type = catalogue.find(session.type);
        final Location anchor = anchor(player);
        if (type.isEmpty() || anchor == null) {
            session.preview.hide();
            return;
        }

        final Optional<Component> problem = construction.problem(player, player.getWorld(), type.get(), anchor,
                session.quarterTurns);
        session.preview.setValid(problem.isEmpty());
        session.preview.show(anchor, session.quarterTurns);
        player.sendActionBar(problem.orElse(Translations.component("core.construction.blueprint.hint")
                .color(NamedTextColor.GREEN)));
    }

    private void end(@NotNull Player player) {
        if (sessions.remove(player.getUniqueId()) != null) {
            previews.close(player);
        }
    }

    /** The block above the one the player is looking at, which is where the structure's anchor goes. */
    private static @Nullable Location anchor(@NotNull Player player) {
        final Block target = player.getTargetBlockExact(REACH);
        return target == null ? null : target.getRelative(BlockFace.UP).getLocation();
    }

    private static final class Session {
        private final String type;
        private final GhostPreview preview;
        private int quarterTurns;

        private Session(@NotNull String type, @NotNull GhostPreview preview, int quarterTurns) {
            this.type = type;
            this.preview = preview;
            this.quarterTurns = quarterTurns;
        }
    }
}
