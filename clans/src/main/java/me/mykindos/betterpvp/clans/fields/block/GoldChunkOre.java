package me.mykindos.betterpvp.clans.fields.block;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.fields.model.CustomOre;
import me.mykindos.betterpvp.clans.fields.model.FieldsBlock;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

/**
 * Handles gold chunks.
 *
 * Gold chunks give a random amount of gold when picked up.
 */
@BPvPListener
public class GoldChunkOre extends CustomOre implements Listener {

    private static final Random random = new Random();

    @Inject
    @Config(path = "fields.blocks.goldchunk.minCoins", defaultValue = "50")
    private int minCoins;

    @Inject
    @Config(path = "fields.blocks.goldchunk.maxCoins", defaultValue = "150")
    private int maxCoins;

    @Inject
    public GoldChunkOre(Clans clans, ClientManager clientManager) {
        super(clans, clientManager);
    }

    @Override
    public String getName() {
        return "Gold Chunk";
    }

    @Override
    public @NotNull BlockData getType() {
        return Material.RAW_GOLD_BLOCK.createBlockData();
    }

    @Override
    public @NotNull BlockData getReplacement() {
        return Material.RAW_IRON_BLOCK.createBlockData();
    }

    @Override
    public ItemStack @NotNull [] generateDrops(@NotNull FieldsBlock fieldsBlock) {
        return new ItemStack[] { getGoldChunk(minCoins, maxCoins) };
    }

    private ItemStack getGoldChunk(int minCoinsIn, int maxCoinsIn) {
        int gold = random.ints(minCoinsIn, maxCoinsIn + 1).findAny().orElse(minCoinsIn);
        Material material = gold - minCoinsIn > (maxCoinsIn - minCoinsIn) / 2 ? Material.GOLD_INGOT : Material.GOLD_NUGGET;

        final ItemStack itemStack = new ItemStack(material, 1);
        final ItemMeta meta = itemStack.getItemMeta();
        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ClansNamespacedKeys.FIELDS_GOLD_CHUNK, PersistentDataType.INTEGER, gold);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPickup(InventoryPickupItemEvent event) {
        final PersistentDataContainer pdc = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer();
        final NamespacedKey key = ClansNamespacedKeys.FIELDS_GOLD_CHUNK;
        if (!pdc.has(key, PersistentDataType.INTEGER)) {
            return; // Not a gold chunk
        }

        // Remove gold chunks from being picked up by blocks
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPick(EntityPickupItemEvent event) {
        final PersistentDataContainer pdc = event.getItem().getItemStack().getItemMeta().getPersistentDataContainer();
        final NamespacedKey key = ClansNamespacedKeys.FIELDS_GOLD_CHUNK;
        if (!pdc.has(key, PersistentDataType.INTEGER)) {
            return; // Not a gold chunk
        }

        final LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            event.setCancelled(true); // Only players can pick up gold chunks
            return;
        }

        final int gold = Objects.requireNonNullElse(pdc.get(key, PersistentDataType.INTEGER), 0);
        Gamer gamer = clientManager.search().online(player).getGamer();

        // Success
        event.getItem().remove();
        event.setCancelled(true);
        gamer.saveProperty(GamerProperty.BALANCE, gamer.getBalance() + gold);

        // Cues
        final Component titleMsg = UtilMessage.deserialize("<yellow>+%s Coins", gold);
        final Title.Times times = Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(1), Duration.ofSeconds(0));
        final Title title = Title.title(Component.empty(), titleMsg, times);
        player.showTitle(title);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        UtilMessage.message(player, "Fields", "You picked up <alt2>%s</alt2> coins!", gold);
    }
}
