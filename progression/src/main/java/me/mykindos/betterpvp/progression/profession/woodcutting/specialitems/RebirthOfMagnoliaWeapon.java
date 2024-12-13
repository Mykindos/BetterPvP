package me.mykindos.betterpvp.progression.profession.woodcutting.specialitems;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import me.mykindos.betterpvp.progression.Progression;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CustomLog
@BPvPListener
@Singleton
public class RebirthOfMagnoliaWeapon extends Weapon implements Listener {
    private final Material[] ALLOWED_BONEMEALABLE_BLOCKS = { Material.GRASS_BLOCK };


    private WeighedList<Material> lootTypes;


    @Inject
    public RebirthOfMagnoliaWeapon(Progression progression) {
        super(progression, "rebirth_of_magnolia");

        // Create recipes for dark green dye? like DiamondSword.java
    }

    @EventHandler
    public void onBonemealUseOnDirt(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();
        if (block == null) return;

        // NOTE TO REFACTOR THIS WHEN YOU REFACTOR ALL OF WOODCUTTING
        // StrippedLogListener repeats the same code (and probably other places in the codebase)
        if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) return;
    }

    @EventHandler
    public void onBonemealGrassBlock(BlockFertilizeEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        if (player == null) return;

        Material clickedBlockType = event.getBlock().getType();
        if (!Arrays.stream(ALLOWED_BONEMEALABLE_BLOCKS).toList().contains(clickedBlockType)) return;

        ItemStack bonemealItem  = event.getPlayer().getInventory().getItemInMainHand();

        if (!bonemealItem.getType().equals(getMaterial())) return;

        // If the item doesn't have item meta, then it's normal bonemeal
        if (!bonemealItem.hasItemMeta()) return;

        ItemMeta itemMeta = bonemealItem.getItemMeta();
        if (!itemMeta.hasCustomModelData()) return;

        if (itemMeta.getCustomModelData() != getModel()) return;

        // Eventually replace this with loot table
        List<BlockState> blockStates = event.getBlocks();
        blockStates.stream()
                .filter(this::isFlower)

                // Replace each flower with a random one
                .forEach(blockState -> blockState.setType(lootTypes.random()));

        // I don't like tall grass so let's replace with a random flower
        blockStates.stream()
                .filter(blockState -> blockState.getType().equals(Material.TALL_GRASS))

                // Tall grass is like a 2-block block, so we only want the bottom one
                // You need to get the block up 1 because event.getBlock() returns the block that was bonemealed
                .filter(blockState -> blockState.getY() == event.getBlock().getRelative(0, 1, 0).getY())
                .forEach(blockState -> blockState.setType(lootTypes.random()));

        // Remove any leftover tall grass
        blockStates.removeIf(blockState -> blockState.getType().equals(Material.TALL_GRASS));
    }

    // MOVE TO UTIL BLOCK EVENTUALLY
    public boolean isFlower(BlockState blockState) {
        // List of all flowers in Minecraft (can expand this list as needed)
        return switch (blockState.getType()) {
            case DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
                    RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
                    OXEYE_DAISY, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE ->
                    true;
            default -> false;
        };
    }
    @Override
    public List<Component> getLore(ItemMeta meta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("Long ago, the Magnolia Sovereign wept for a"));
        lore.add(UtilMessage.deserialize("fading world. Her tears blessed barren soil"));
        lore.add(UtilMessage.deserialize("with life, giving rise to extraordinary flowers."));
        lore.add(UtilMessage.deserialize("This essence carries her power, blooming <orange>rare</orange>"));
        lore.add(UtilMessage.deserialize("<orange>flowers</orange> wherever it touches the ground."));
        return lore;
    }

    @Override
    public void loadWeaponConfig() {
        lootTypes = new WeighedList<>();

        ConfigurationSection lootSection = getConfigObject("loot", null, ConfigurationSection.class);

        // Don't know how to create the section from here
        if (lootSection == null) {
            return;
        }


        for (String lootItemKey : lootSection.getKeys(false)) {
            ConfigurationSection lootItemSection = lootSection.getConfigurationSection(lootItemKey);
            if (lootItemSection == null) continue;

            String itemMaterialAsString = lootItemSection.getString("material");
            if (itemMaterialAsString == null) continue;

            Material material = Material.getMaterial(itemMaterialAsString.toUpperCase());
            if (material == null) continue;

            int frequency = lootItemSection.getInt("frequency");

            lootTypes.add(frequency, 1, material);
        }

        log.info("Loaded " + lootTypes.size() + " flower loot types").submit();
    }
}
