package me.mykindos.betterpvp.core.block.custom;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.block.SmartBlock;
import me.mykindos.betterpvp.core.block.SmartBlockBreakOverride;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockRegistry;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.title.TitleComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Smart blocks drawn by the BetterPvP resource pack: furniture, an item display tagged with its block id and barrier
 * blocks for collision, and custom note blocks, whose state the release maps to a block id.
 */
@Singleton
public class PackSmartBlockFactory implements SmartBlockFactory {

    static final NamespacedKey FURNITURE = new NamespacedKey("betterpvp", "furniture");

    private final SmartBlockRegistry registry;
    private final SmartBlockDataManager dataManager;
    private final NoteBlockStates states;
    private final ItemRegistry itemRegistry;
    private final Provider<ItemFactory> itemFactory;
    private final ClientManager clientManager;
    private final Set<Block> breaking = new HashSet<>();

    @Inject
    public PackSmartBlockFactory(SmartBlockRegistry registry, SmartBlockDataManager dataManager, NoteBlockStates states,
                                 ItemRegistry itemRegistry, Provider<ItemFactory> itemFactory, ClientManager clientManager) {
        this.registry = registry;
        this.dataManager = dataManager;
        this.states = states;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
    }

    public Optional<SmartBlock> byBlockId(String blockId) {
        return registry.getAllBlocks().values().stream()
                .filter(block -> blockId.equals(blockIdOf(block)))
                .findFirst();
    }

    private static String blockIdOf(SmartBlock block) {
        if (block instanceof FurnitureBlock furniture) {
            return furniture.getBlockId();
        }
        if (block instanceof CustomNoteBlock noteBlock) {
            return noteBlock.getBlockId();
        }
        return null;
    }

    /**
     * The furniture display a block belongs to: its base block, a barrier or a light of it.
     */
    public Optional<ItemDisplay> furnitureAt(Block block) {
        for (ItemDisplay display : block.getLocation().toCenterLocation().getNearbyEntitiesByType(ItemDisplay.class, 4.0)) {
            final Optional<FurnitureBlock> furniture = furnitureOf(display);
            if (furniture.isPresent() && occupied(display, furniture.get().getShape()).contains(block)) {
                return Optional.of(display);
            }
        }
        return Optional.empty();
    }

    private Optional<FurnitureBlock> furnitureOf(Entity entity) {
        if (!(entity instanceof ItemDisplay display)) {
            return Optional.empty();
        }
        final String blockId = display.getPersistentDataContainer().get(FURNITURE, PersistentDataType.STRING);
        if (blockId == null) {
            return Optional.empty();
        }
        return byBlockId(blockId).filter(FurnitureBlock.class::isInstance).map(FurnitureBlock.class::cast);
    }

    /**
     * The base block of a piece of furniture, with every barrier and light block it places.
     */
    private Set<Block> occupied(ItemDisplay display, FurnitureShape shape) {
        final Block base = display.getLocation().getBlock();
        final Set<Block> blocks = new HashSet<>();
        blocks.add(base);
        for (Vector offset : shape.getBarriers()) {
            blocks.add(relative(base, offset, display.getYaw()));
        }
        for (Vector offset : shape.getLights().keySet()) {
            blocks.add(relative(base, offset, display.getYaw()));
        }
        return blocks;
    }

    private static Block relative(Block base, Vector offset, float yaw) {
        final Vector turned = FurnitureShape.rotate(offset, yaw);
        return base.getRelative(turned.getBlockX(), turned.getBlockY(), turned.getBlockZ());
    }

    private SmartBlockInstance instance(SmartBlock type, Block block) {
        return new SmartBlockInstance(type, block, dataManager);
    }

    @Override
    public Optional<SmartBlockInstance> from(Location location) {
        return from(location.getBlock());
    }

    @Override
    public Optional<SmartBlockInstance> from(Block block) {
        final Optional<String> noteBlock = states.blockOf(block.getBlockData());
        if (noteBlock.isPresent()) {
            return byBlockId(noteBlock.get()).map(type -> instance(type, block));
        }
        if (block.getType() == Material.NOTE_BLOCK) {
            return Optional.empty();
        }
        return furnitureAt(block).flatMap(this::fromDisplay);
    }

    private Optional<SmartBlockInstance> fromDisplay(ItemDisplay display) {
        return furnitureOf(display).map(furniture -> instance((SmartBlock) furniture, display.getLocation().getBlock()));
    }

    public Optional<SmartBlockInstance> from(Entity entity) {
        return entity instanceof ItemDisplay display ? fromDisplay(display) : Optional.empty();
    }

    @Override
    public Optional<SmartBlockInstance> fromTarget(Player player) {
        final double range = Objects.requireNonNull(player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)).getValue();
        final RayTraceResult result = player.rayTraceBlocks(range);
        if (result == null || result.getHitBlock() == null) {
            return Optional.empty();
        }
        return from(result.getHitBlock());
    }

    @Override
    public Optional<SmartBlockInstance> load(Block block) {
        return from(block);
    }

    @Override
    public boolean isSmartBlock(Block block) {
        return from(block).isPresent();
    }

    @Override
    public boolean isSmartBlock(Location location) {
        return from(location).isPresent();
    }

    @Override
    public boolean isSmartBlock(Entity entity) {
        return from(entity).isPresent();
    }

    @Override
    public boolean isTargetSmartBlock(@NotNull Player player) {
        return fromTarget(player).isPresent();
    }

    @Override
    public BlockData createBlockData(SmartBlock type) {
        if (!(type instanceof CustomNoteBlock noteBlock)) {
            throw new IllegalArgumentException(type.getKey() + " is not a custom note block");
        }
        return states.stateOf(noteBlock.getBlockId())
                .orElseThrow(() -> new IllegalStateException("The resource pack release has no state for " + noteBlock.getBlockId()));
    }

    /**
     * Places a smart block for a player, as if they placed a vanilla block: protections can cancel the
     * {@link BlockPlaceEvent}. Furniture faces the player and needs room for every block it takes.
     */
    public Optional<SmartBlockInstance> place(Player player, SmartBlock type, Block target, ItemStack item, Block against) {
        final BlockPlaceEvent event = new BlockPlaceEvent(target, target.getState(), against, item, player, true, EquipmentSlot.HAND);
        if (!event.callEvent() || !event.canBuild()) {
            return Optional.empty();
        }
        if (type instanceof CustomNoteBlock noteBlock) {
            final Optional<BlockData> state = states.stateOf(noteBlock.getBlockId());
            if (state.isEmpty()) {
                return Optional.empty();
            }
            target.setBlockData(state.get(), false);
            playSound(target, noteBlock.getSounds().createBlockData().getSoundGroup().getPlaceSound());
            return Optional.of(instance(type, target));
        }
        if (!(type instanceof FurnitureBlock furniture)) {
            return Optional.empty();
        }
        final FurnitureShape shape = furniture.getShape();
        final float yaw = Math.round((player.getYaw() + 180f) / 90f) * 90f;
        final List<Block> barriers = new ArrayList<>();
        for (Vector offset : shape.getBarriers()) {
            barriers.add(relative(target, offset, yaw));
        }
        if (!target.isReplaceable() || barriers.stream().anyMatch(block -> !block.equals(target) && !block.isReplaceable())) {
            return Optional.empty();
        }
        final ItemStack model = ItemStack.of(Material.PAPER);
        model.editMeta(meta -> meta.setItemModel(new NamespacedKey(shape.getModel().namespace(), shape.getModel().value())));
        final Location at = target.getLocation().add(0.5, 0, 0.5);
        at.setYaw(yaw);
        // FIXED renders like a floor item frame: the models' fixed display stands them up again
        at.setPitch(-90f);
        target.getWorld().spawn(at, ItemDisplay.class, display -> {
            display.setItemStack(model);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(new Transformation(shape.getTranslation(), new Quaternionf(), shape.getScale(), new Quaternionf()));
            display.getPersistentDataContainer().set(FURNITURE, PersistentDataType.STRING, furniture.getBlockId());
            display.setPersistent(true);
        });
        barriers.forEach(block -> block.setType(Material.BARRIER, false));
        shape.getLights().forEach((offset, level) -> {
            final Block block = relative(target, offset, yaw);
            if (block.getType().isAir()) {
                final Light light = (Light) Material.LIGHT.createBlockData();
                light.setLevel(level);
                block.setBlockData(light, false);
            }
        });
        playSound(target, shape.getSounds().createBlockData().getSoundGroup().getPlaceSound());
        final SmartBlockInstance instance = instance(type, target);
        dataManager.getProvider().getOrCreateData(instance);
        return Optional.of(instance);
    }

    /**
     * True while this factory breaks the block, so listeners can tell its own break event from a vanilla one.
     */
    public boolean isBreaking(Block block) {
        return breaking.contains(block);
    }

    @Override
    public boolean breakBlock(Player player, SmartBlockInstance instance) {
        final Block block = instance.getHandle();
        final SmartBlock type = instance.getType();
        final BlockBreakEvent event = new BlockBreakEvent(block, player);
        breaking.add(block);
        try {
            if (!event.callEvent()) {
                return false;
            }
        } finally {
            breaking.remove(block);
        }
        if (type instanceof FurnitureBlock furniture) {
            final Optional<ItemDisplay> display = furnitureAt(block);
            if (display.isEmpty()) {
                return false;
            }
            remove(display.get(), furniture.getShape());
            playSound(block, furniture.getShape().getSounds().createBlockData().getSoundGroup().getBreakSound());
        } else if (type instanceof CustomNoteBlock noteBlock) {
            block.setType(Material.AIR);
            playSound(block, noteBlock.getSounds().createBlockData().getSoundGroup().getBreakSound());
        } else {
            return false;
        }
        dataManager.removeData(instance, BlockRemovalCause.NATURAL);
        final boolean harvested = !(type instanceof CustomNoteBlock noteBlock)
                || noteBlock.canHarvest(player.getInventory().getItemInMainHand());
        if (event.isDropItems() && harvested && player.getGameMode() != GameMode.CREATIVE) {
            drop(type, block);
        }
        return true;
    }

    /**
     * Removes a piece of furniture's display, barriers and lights, without drops.
     */
    public void remove(ItemDisplay display, FurnitureShape shape) {
        final Block base = display.getLocation().getBlock();
        for (Vector offset : shape.getBarriers()) {
            final Block block = relative(base, offset, display.getYaw());
            if (block.getType() == Material.BARRIER) {
                block.setType(Material.AIR, false);
            }
        }
        for (Vector offset : shape.getLights().keySet()) {
            final Block block = relative(base, offset, display.getYaw());
            if (block.getType() == Material.LIGHT) {
                block.setType(Material.AIR, false);
            }
        }
        display.remove();
    }

    public void drop(SmartBlock type, Block block) {
        final String blockId = blockIdOf(type);
        itemRegistry.getItems().values().stream()
                .filter(item -> item instanceof PlacesBlock places && places.getBlockId().equals(blockId))
                .findFirst()
                .map(item -> itemFactory.get().create((BaseItem) item).createItemStack())
                .ifPresent(stack -> block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), stack));
    }

    private static void playSound(Block block, Sound sound) {
        block.getWorld().playSound(block.getLocation().toCenterLocation(), sound, 1f, 1f);
    }

    @Override
    public @NotNull SmartBlockBreakOverride getBreakOverrideDefaults(@NotNull SmartBlockInstance instance,
                                                                      @NotNull Player player,
                                                                      @NotNull ItemStack held) {
        if (instance.getType() instanceof FurnitureBlock furniture) {
            return SmartBlockBreakOverride.builder().hardness(furniture.getShape().getHardness()).build();
        }
        if (instance.getType() instanceof CustomNoteBlock noteBlock) {
            return SmartBlockBreakOverride.builder().hardness(noteBlock.getHardness()).build();
        }
        return SmartBlockBreakOverride.empty();
    }

    @Override
    public void displayBreakProgress(@NotNull Player player, @NotNull Block block, double progress) {
        if (block.getType() != Material.BARRIER) {
            return;
        }
        final float clamped = (float) Math.max(0.0, Math.min(1.0, progress));
        final TextComponent bar = ProgressBar.withProgress(clamped)
                .withProgressColor(NamedTextColor.WHITE)
                .withRemainingColor(NamedTextColor.DARK_GRAY)
                .withCharacter(' ')
                .build()
                .decorate(TextDecoration.STRIKETHROUGH);
        final Gamer gamer = clientManager.search().online(player).getGamer();
        gamer.getTitleQueue().add(500, TitleComponent.subtitle(0.0, 0.2, 0.0, false, g -> bar));
    }

    /**
     * The face a player clicked is where a block they place goes, unless the clicked block can be replaced.
     */
    static Block placementTarget(Block clicked, BlockFace face) {
        return clicked.isReplaceable() ? clicked : clicked.getRelative(face);
    }

}
