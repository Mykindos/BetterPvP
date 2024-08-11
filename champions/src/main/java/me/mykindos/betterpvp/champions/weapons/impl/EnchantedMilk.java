package me.mykindos.betterpvp.champions.weapons.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.weapon.Weapon;
import me.mykindos.betterpvp.core.combat.weapon.types.ChannelWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.CooldownWeapon;
import me.mykindos.betterpvp.core.combat.weapon.types.InteractWeapon;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class EnchantedMilk extends Weapon implements InteractWeapon, CooldownWeapon{

    private final EffectManager effectManager;
    private double duration;

    @Inject
    private CooldownManager cooldownManager;

    @Inject
    public EnchantedMilk(Champions champions, EffectManager effectManager) {
        super(champions, "enchanted_milk");
        this.effectManager = effectManager;
    }

    @Override
    public void activate(Player player) {
        cooldownManager.removeCooldown(player, getSimpleName(), true);
    }

    @EventHandler
    public void onDrink(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        if (!canUse(player)) return;

        UtilMessage.message(player, "Item",
                Component.text("You consumed ", NamedTextColor.GRAY).append(getName().color(NamedTextColor.YELLOW)));
        UtilSound.playSound(player, Sound.ENTITY_GENERIC_DRINK, 1f, 1f, false);
        UtilSound.playSound(player.getWorld(), player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 1.2f);
        UtilInventory.remove(player, getMaterial(), 1);

        this.effectManager.addEffect(player, EffectTypes.IMMUNE, (long) (duration * 1000L));
        cooldownManager.use(player, getSimpleName(), getCooldown(), true);

        UtilServer.callEvent(new EffectClearEvent(player));
    }

    @Override
    public List<Component> getLore(ItemMeta itemMeta) {
        List<Component> lore = new ArrayList<>();
        lore.add(UtilMessage.deserialize("<gray>Cleanses negative effects"));
        return lore;
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
    public void loadWeaponConfig() {
        duration = getConfig("duration", 1.5, Double.class);
    }

    @Override
    public boolean showCooldownOnItem() {
        return false;
    }
}
