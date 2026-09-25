package me.mykindos.betterpvp.core.world.construction.blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.component.PermanentComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleQueue;
import me.mykindos.betterpvp.core.world.construction.ConstructionResult;
import me.mykindos.betterpvp.core.world.construction.ConstructionService;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Holding a blueprint in the main hand shows a ghost of its structure on the block the player looks at, green where it
 * can be built and red where it cannot. The structure's name is the title, what to do or that it cannot go there is
 * the subtitle, and the action bar shows how to turn it or exactly why it does not fit. Each press of sneak turns it a
 * quarter, and right-click builds it there, using the blueprint up. A blueprint bound to a placed structure moves that
 * structure there instead.
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
    private final ClientManager clientManager;

    private final Map<UUID, Session> sessions = new HashMap<>();

    @Inject
    public BlueprintSessions(@NotNull StructureBlueprintItem blueprint, @NotNull ItemFactory itemFactory,
                             @NotNull StructureCatalogue catalogue, @NotNull SchematicService schematics,
                             @NotNull ConstructionService construction, @NotNull GhostPreviews previews,
                             @NotNull ClientManager clientManager) {
        this.blueprint = blueprint;
        this.itemFactory = itemFactory;
        this.catalogue = catalogue;
        this.schematics = schematics;
        this.construction = construction;
        this.previews = previews;
        this.clientManager = clientManager;
    }

    /** A blueprint for {@code type}. */
    public @NotNull ItemStack blueprintFor(@NotNull StructureType type) {
        return itemFactory.create(blueprint).withComponent(new StructureBlueprintComponent(type.getId())).createItemStack();
    }

    /** A blueprint that moves the placed structure {@code id} of {@code type} to wherever it is used. */
    public @NotNull ItemStack blueprintToMove(@NotNull StructureType type, @NotNull UUID id) {
        return itemFactory.create(blueprint).withComponent(new StructureBlueprintComponent(type.getId(), id))
                .createItemStack();
    }

    /** Which structure {@code stack} is a blueprint for, if it is one. */
    public @NotNull Optional<StructureType> structureOf(@Nullable ItemStack stack) {
        return componentOf(stack).flatMap(component -> catalogue.find(component.getStructure()));
    }

    private @NotNull Optional<StructureBlueprintComponent> componentOf(@Nullable ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }
        return itemFactory.fromItemStack(stack)
                .filter(instance -> instance.getBaseItem() == blueprint)
                .flatMap(instance -> instance.getComponent(StructureBlueprintComponent.class))
                .filter(component -> catalogue.find(component.getStructure()).isPresent());
    }

    @UpdateEvent(delay = 100)
    public void follow() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final Optional<StructureBlueprintComponent> held = componentOf(player.getInventory().getItemInMainHand());
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
        final Optional<StructureBlueprintComponent> held = componentOf(player.getInventory().getItemInMainHand());
        final Optional<StructureType> type = held.flatMap(component -> catalogue.find(component.getStructure()));
        if (held.isEmpty() || type.isEmpty()) {
            return;
        }
        event.setCancelled(true);

        final Session session = sessions.get(player.getUniqueId());
        final Location anchor = anchor(player);
        if (session == null || anchor == null) {
            return;
        }

        final UUID moving = held.get().getMoving();
        final ConstructionResult result = moving == null
                ? construction.build(player, player.getWorld(), type.get(), anchor, session.quarterTurns)
                : construction.move(player, player.getWorld(), moving, anchor, session.quarterTurns);
        if (!result.isSuccess()) {
            if (result.getReason() != null) {
                UtilMessage.plain(player, result.getReason());
            }
            return;
        }

        player.getInventory().getItemInMainHand().subtract();
        end(player);
        UtilMessage.plain(player, Translations.component(moving == null ? "core.construction.blueprint.started"
                        : "core.construction.blueprint.move_started", type.get().getDisplayName().color(NamedTextColor.WHITE))
                .color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        end(event.getPlayer());
    }

    private @NotNull Optional<Session> session(@NotNull Player player, @NotNull StructureBlueprintComponent held) {
        final Session existing = sessions.get(player.getUniqueId());
        if (existing != null && existing.type.equals(held.getStructure())
                && Objects.equals(existing.moving, held.getMoving())) {
            return Optional.of(existing);
        }

        final Optional<StructureType> type = catalogue.find(held.getStructure());
        if (type.isEmpty()) {
            return Optional.empty();
        }
        final int stage = held.getMoving() == null ? 0 : construction.worksite(player.getWorld())
                .flatMap(worksite -> worksite.getHolding().find(held.getMoving()))
                .map(PlacedStructure::getStage)
                .orElse(0);
        final Optional<Schematic> schematic = schematics.load(type.get().stage(stage).getSchematic());
        if (schematic.isEmpty()) {
            return Optional.empty();
        }
        end(player);
        final int facing = BlockTransform.quarterTurnsBetween(schematic.get().getAnchorYaw(), player.getLocation().getYaw());
        final Session session = new Session(held.getStructure(), held.getMoving(),
                previews.open(player, schematic.get()), clientManager.search().online(player).getGamer(), facing);
        sessions.put(player.getUniqueId(), session);
        return Optional.of(session);
    }

    private void update(@NotNull Player player, @NotNull Session session) {
        final Optional<StructureType> type = catalogue.find(session.type);
        if (type.isEmpty()) {
            session.preview.hide();
            return;
        }
        final Location anchor = anchor(player);
        final Optional<Component> problem;
        if (anchor == null) {
            session.preview.hide();
            problem = Optional.of(Translations.component("core.construction.blueprint.look_at_ground")
                    .color(NamedTextColor.RED));
        } else {
            problem = session.moving == null
                    ? construction.problem(player, player.getWorld(), type.get(), anchor, session.quarterTurns)
                    : construction.moveProblem(player, player.getWorld(), session.moving, anchor, session.quarterTurns);
            session.preview.setValid(problem.isEmpty());
            session.preview.show(anchor, session.quarterTurns);
        }

        if (problem.isPresent()) {
            hud(session, type.get().getDisplayName().color(NamedTextColor.RED),
                    Translations.component("core.construction.blueprint.cannot_place").color(NamedTextColor.RED),
                    problem.get());
            return;
        }
        hud(session, type.get().getDisplayName().color(NamedTextColor.GREEN),
                Translations.component(session.moving == null
                        ? "core.construction.blueprint.hint" : "core.construction.blueprint.move_hint")
                        .color(NamedTextColor.GRAY),
                Translations.component("core.construction.blueprint.turn_hint").color(NamedTextColor.GRAY));
    }

    /**
     * Shows {@code title} over {@code subtitle}, with {@code actionBar} below, for as long as the blueprint is held.
     * <p>
     * The action bar is re-sent every tick from the session, so it only needs its text swapped. A title is sent once and
     * then runs out on the client, so it is sent with a long stay and replaced only when its text changes or well
     * before that stay ends. Replacing it any later lets the client start fading it out first, which flickers.
     */
    private static void hud(@NotNull Session session, @NotNull Component title, @NotNull Component subtitle,
                            @NotNull Component actionBar) {
        session.actionBarText = actionBar;

        final TitleComponent current = session.title;
        if (current != null && !current.isInvalid() && current.getRemaining() > 2500
                && title.equals(session.titleText) && subtitle.equals(session.subtitleText)) {
            return;
        }
        if (current != null) {
            session.gamer.getTitleQueue().remove(current);
        }
        session.titleText = title;
        session.subtitleText = subtitle;
        session.title = new TitleComponent(0, 5, 0.25, false, gamer -> title, gamer -> subtitle);
        session.gamer.getTitleQueue().add(10, session.title);
    }

    private void end(@NotNull Player player) {
        final Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        previews.close(player);
        session.gamer.getActionBar().remove(session.actionBar);
        final TitleComponent title = session.title;
        if (title != null) {
            final TitleQueue titles = session.gamer.getTitleQueue();
            titles.remove(title);
            // The client keeps a title up until its stay runs out, which is seconds after the blueprint is put away.
            if (titles.isShowing(title)) {
                player.clearTitle();
            }
        }
    }

    /** The block above the one the player is looking at, which is where the structure's anchor goes. */
    private static @Nullable Location anchor(@NotNull Player player) {
        final Block target = player.getTargetBlockExact(REACH);
        return target == null ? null : target.getRelative(BlockFace.UP).getLocation();
    }

    private static final class Session {
        private final String type;
        private final @Nullable UUID moving;
        private final GhostPreview preview;
        private final Gamer gamer;
        /** Read by the action bar every tick, off the main thread. */
        private final PermanentComponent actionBar = new PermanentComponent(viewer -> this.actionBarText);
        private volatile @Nullable Component actionBarText;
        private int quarterTurns;
        private @Nullable TitleComponent title;
        private @Nullable Component titleText;
        private @Nullable Component subtitleText;

        private Session(@NotNull String type, @Nullable UUID moving, @NotNull GhostPreview preview,
                        @NotNull Gamer gamer, int quarterTurns) {
            this.type = type;
            this.moving = moving;
            this.preview = preview;
            this.gamer = gamer;
            this.quarterTurns = quarterTurns;
            gamer.getActionBar().add(50, actionBar);
        }
    }
}
