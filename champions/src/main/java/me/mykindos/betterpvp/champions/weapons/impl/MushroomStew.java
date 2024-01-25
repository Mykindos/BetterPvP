package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class MushroomStew extends Weapon implements InteractWeapon, CooldownWeapon, Listener {


    @Inject
    @Config(path = "weapons.mushroom-stew.cooldown", defaultValue = "14.0", configName = "weapons/standard")
    private double cooldown;

    @Inject
    @Config(path = "weapons.mushroom-stew.duration", defaultValue = "4.0", configName = "weapons/standard")
    private double duration;

    @Inject
    @Config(path = "weapons.mushroom-stew.level", defaultValue = "2", configName = "weapons/standard")
    private int level;

    @Inject
    public MushroomStew() {
        super("mushroom_stew");
    }

    @Override
    public void activate(Player player) {

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, (int) (duration * 20L), level - 1));
        UtilMessage.message(player, "Item",
                Component.text("You consumed a ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        UtilInventory.remove(player, getMaterial(), 1);

    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCraftStew(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if(recipe == null) return;

        if(recipe.getResult().getType() == Material.MUSHROOM_STEW) {

            ItemStack item = getItemStack();
            item.editMeta(meta -> meta.getPersistentDataContainer().set(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING, getIdentifier()));
            event.getInventory().setResult(item);

        }
    }

    @Override
    public boolean canUse(Player player) {
        return isHoldingWeapon(player);
    }

    @Override
    public double getCooldown() {
        return cooldown;
    }
}
