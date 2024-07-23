package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

@Singleton
@BPvPListener
public class MushroomStew extends Weapon implements InteractWeapon, CooldownWeapon, Listener {

    private final EffectManager effectManager;

    private double duration;
    private int level;

    @Inject
    public MushroomStew(Champions champions, EffectManager effectManager) {
        super(champions, "mushroom_stew");
        this.effectManager = effectManager;
    }

    @Override
    public void activate(Player player) {
        effectManager.addEffect(player, EffectTypes.REGENERATION, level, (long) (duration * 1000));
        UtilMessage.message(player, "Item",
                Component.text("You consumed a ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ENTITY_GENERIC_EAT, 2f, 1f, false);
        UtilSound.playSound(player, Sound.BLOCK_GRASS_BREAK, 1f, 1f, false);
        UtilSound.playSound(player, Sound.BLOCK_GRASS_BREAK, 1f, 1f, false);
        UtilInventory.remove(player, getMaterial(), 1);

    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Grants <white>Regeneration %s</white> for <yellow>%.1f seconds</yellow>", UtilFormat.getRomanNumeral(level), duration));
        return lore;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onCraftStew(PrepareItemCraftEvent event) {
        if (!enabled) {
            return;
        }
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

    @Override
    public boolean showCooldownOnItem() {
        return true;
    }

    @Override
    public void loadWeaponConfig() {
        duration = getConfig("duration", 4.0, Double.class);
        level = getConfig("level", 2, Integer.class);
    }
}
