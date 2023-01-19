package me.mykindos.betterpvp.core.items;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.items.enchants.GlowEnchant;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.weapons.WeaponManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.util.RGBLike;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ItemHandler {

    private final WeaponManager weaponManager;
    private final ItemRepository itemRepository;
    private HashMap<Material, BPVPItem> itemMap = new HashMap<>();

    @Inject
    @Config(path = "items.hideAttributes", defaultValue = "true")
    private boolean hideAttributes;

    @Inject
    public ItemHandler(Core core, WeaponManager weaponManager, ItemRepository itemRepository) {
        this.weaponManager = weaponManager;
        this.itemRepository = itemRepository;

        NamespacedKey key = new NamespacedKey(core, core.getDescription().getName());
        registerEnchantment(new GlowEnchant(key));
    }

    public void loadItemData(String module) {
        List<BPVPItem> items = itemRepository.getItemsForModule(module);
        items.forEach(item -> itemMap.put(item.getMaterial(), item));
    }

    /**
     * General method that updates the name of almost every item that is picked up by players
     * E.g. Names leather armour after assassins
     * E.g. Turns the colour of the items name to yellow from white
     *
     * @param itemStack ItemStack to update
     * @return An ItemStack with an updated name
     */
    public ItemStack updateNames(ItemStack itemStack) {

        Material material = itemStack.getType();
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (hideAttributes) {
            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        BPVPItem item = itemMap.get(material);
        if (item != null) {
            itemMeta.displayName(item.getName());
            itemMeta.lore(item.getLore());

            if (item.isGlowing()) {
                UtilItem.addGlow(itemStack);
            }else{
                for(Map.Entry<Enchantment, Integer> entry : itemStack.getEnchantments().entrySet()){
                    itemStack.removeEnchantment(entry.getKey());
                }
            }
        }else{
            itemMeta.displayName(Component.text(ChatColor.YELLOW + UtilFormat.cleanString(material.name())));
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    private void registerEnchantment(Enchantment enchantment) {
        try {
            Field accept = Enchantment.class.getDeclaredField("acceptingNew");
            accept.setAccessible(true);
            accept.set(null, true);
            Enchantment.registerEnchantment(enchantment);
        }catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
