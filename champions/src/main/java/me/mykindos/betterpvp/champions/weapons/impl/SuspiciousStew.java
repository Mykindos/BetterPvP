package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.EffectType;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Singleton
@BPvPListener
public class SuspiciousStew extends Weapon implements InteractWeapon, CooldownWeapon, Listener {

    private final EffectManager effectManager;
    private static final Random random = new Random();
    private double duration;

    @Inject
    public SuspiciousStew(Champions champions, EffectManager effectManager) {
        super(champions, "suspicious_stew");
        this.effectManager = effectManager;
    }

    @Override
    public void activate(Player player) {
        List<EffectType> effectTypesList = EffectTypes.getEffectTypes();

        List<EffectType> validEffectTypes = effectTypesList.stream()
                .filter(effect -> !effect.equals(EffectTypes.STUN) && !effect.equals(EffectTypes.FROZEN))
                .toList();

        EffectType randomEffect = validEffectTypes.get(random.nextInt(validEffectTypes.size()));

        int randomLevel = random.nextInt(4) + 1;

        effectManager.addEffect(player, randomEffect, randomLevel, (long) (duration * 1000));
        UtilMessage.message(player, "Item",
                Component.text("You consumed a ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));

        UtilMessage.message(player, "Effect",
                Component.text("You have been granted ", NamedTextColor.GREEN)
                        .append(Component.text(randomEffect.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" Level " + randomLevel, NamedTextColor.GREEN))
                        .append(Component.text(" for " + duration + " seconds.", NamedTextColor.GREEN)));

        UtilSound.playSound(player, Sound.ENTITY_PLAYER_BURP, 1f, 1f, false);
        UtilInventory.remove(player, getMaterial(), 1);
    }


    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Grants a random effect for <yellow>%.1f seconds</yellow>", duration));
        return lore;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftStew(PrepareItemCraftEvent event) {
        if (!enabled) {
            return;
        }
        Recipe recipe = event.getRecipe();
        if (recipe == null) return;

        if (recipe.getResult().getType() == Material.SUSPICIOUS_STEW) {

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
        duration = getConfig("duration", 5.0, Double.class);
    }
}
