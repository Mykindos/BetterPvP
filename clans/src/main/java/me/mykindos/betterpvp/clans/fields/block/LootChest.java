package me.mykindos.betterpvp.clans.fields.block;

import com.google.common.base.Preconditions;
import me.mykindos.betterpvp.clans.clans.events.TerritoryInteractEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.impl.clans.FieldsInteractableStat;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.WeighedList;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.Set;

@BPvPListener
public class LootChest implements FieldsInteractable, Listener {

    private static final Random random = new Random();

    private double respawnDelay = 0;
    private final WeighedList<Pair<Material, Range<Integer>>> drops = new WeighedList<>();
    private int dropCount = 1;

    @Override
    public String getName() {
        return "Loot Chest";
    }

    @Override
    public boolean processInteraction(ClientManager clientManager, TerritoryInteractEvent event, FieldsBlock block, ItemFactory itemFactory) {
        if (!event.getInteractionType().equals(TerritoryInteractEvent.InteractionType.INTERACT)) {
            return false; // They didn't right-click the chest
        }

        // Drop the items
        for (int i = 0; i < dropCount; i++) {
            final Pair<Material, Range<Integer>> randomDrop = drops.random();
            if (randomDrop == null) {
                UtilMessage.message(event.getPlayer(), "Fields", "<red>There are no drops configured for this chest.");
                return false; // No drops loaded
            }

            final Material material = randomDrop.getLeft();
            final Range<Integer> amtRange = randomDrop.getRight();
            final int randomAmount = random.ints(amtRange.getMinimum(), amtRange.getMaximum() + 1).findAny().orElse(1);

            final ItemStack drop = new ItemStack(material, randomAmount);
            final Player player = event.getPlayer();
            UtilItem.insert(player, itemFactory.convertItemStack(drop).orElse(drop));

        }

        // Block break particles
        event.getBlock().getWorld().playEffect(event.getBlock().getLocation(), Effect.STEP_SOUND, event.getBlock().getType().createBlockData());
        FieldsInteractableStat stat = FieldsInteractableStat.builder().name(getName()).build();
        clientManager.incrementStat(event.getPlayer(), stat, 1);
        return true;
    }

    @Override
    public void loadConfig(ExtendedYamlConfiguration config) {
        dropCount = config.getOrSaveInt("fields.blocks.lootchest.dropCount", 1);
        ConfigurationSection section = config.getConfigurationSection("fields.blocks.lootchest.drops");
        if (section == null) {
            section = config.createSection("fields.blocks.lootchest.drops");
        }

        final Set<String> keys = section.getKeys(false);
        for (String key : keys) {
            final Material material = Material.getMaterial(key);
            if (material == null) {
                throw new IllegalArgumentException("Invalid material: " + key);
            }

            final int categoryWeight = section.getInt("." + key + ".categoryWeight");
            final int weight = section.getInt("." + key + ".weight");
            final int min = section.getInt("." + key + ".min", 1);
            final int max = section.getInt("." + key + ".max", 1);

            Preconditions.checkArgument(categoryWeight > 0, "Category weight must be greater than 0");
            Preconditions.checkArgument(weight > 0, "Weight must be greater than 0");
            Preconditions.checkArgument(min > 0, "Min must be greater than 0");
            Preconditions.checkArgument(max > 0, "Max must be greater than 0");
            Preconditions.checkArgument(min <= max, "Min must be less than or equal to max");

            final Range<Integer> range = Range.of(min, max);
            drops.add(categoryWeight, weight, Pair.of(material, range));
        }
    }

    @Override
    public @NotNull BlockData getType() {
        return Material.ENDER_CHEST.createBlockData();
    }

    @Override
    public @NotNull BlockData getReplacement() {
        return Material.AIR.createBlockData();
    }

    @Override
    public double getRespawnDelay() {
        return respawnDelay;
    }

    @Override
    public void setRespawnDelay(double delay) {
        this.respawnDelay = delay;
    }

}
