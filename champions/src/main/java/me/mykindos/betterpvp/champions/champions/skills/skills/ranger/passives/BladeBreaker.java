package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

@Singleton
@BPvPListener
public class BladeBreaker extends Skill implements PassiveSkill, DebuffSkill {

    @Inject
    private CooldownManager cooldownManager;
    public int weaknessStrength;
    public double weaknessDuration;
    public double weaknessDurationIncreasePerLevel;
    public double internalCooldown;
    public double internalCooldownDecreasePerLevel;

    private static final NamespacedKey WEAKNESS_KEY = new NamespacedKey("blade_breaker_weakness", "betterpvp");

    @Inject
    public BladeBreaker(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Blade Breaker";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Your axe attacks break the enemy's current item, inflicting",
                "<effect>Weakness " + UtilFormat.getRomanNumeral(getWeaknessStrength(level)) + "</effect> for " + getValueString(this::getDuration, level) + " seconds while they continue to hold it",
                "",
                "Internal cooldown of " + getValueString(this::getInternalCooldown, level) + " seconds"
        };
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    public double getDuration(int level) {
        return weaknessDuration + ((level - 1) * weaknessDurationIncreasePerLevel);
    }

    public double getInternalCooldown(int level) {
        return internalCooldown - ((level - 1) * internalCooldownDecreasePerLevel);
    }

    public int getWeaknessStrength(int level) {
        return weaknessStrength;
    }

    @EventHandler
    public void onHit(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getDamagee() instanceof Player target)) return;

        ItemStack heldItem = target.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            return;
        }

        int level = getLevel(player);
        if (level <= 0) return;

        if (player.getInventory().getItemInMainHand().getType() != Material.IRON_AXE &&
                player.getInventory().getItemInMainHand().getType() != Material.DIAMOND_AXE) {
            return;
        }

        if (cooldownManager.hasCooldown(player, getName())) return;

        double effectDuration = getDuration(level);

        applyWeakness(target, heldItem, effectDuration);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0F, 1.0F);
        UtilMessage.simpleMessage(player, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %d</alt>.", event.getDamagee().getName(), getName(), level);
        event.addReason(getName());

        cooldownManager.use(player, getName(), getInternalCooldown(level), true);
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (hasWeaknessPersistentData(newItem)) {
            reapplyWeakness(player, newItem);
        } else {
            removeWeakness(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeWeakness(event.getPlayer());
    }

    @UpdateEvent
    public void update() {
        for (Player player : champions.getServer().getOnlinePlayers()) {
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            if (hasWeaknessPersistentData(heldItem)) {
                long expirationTime = getWeaknessExpirationTime(heldItem);
                if (System.currentTimeMillis() > expirationTime) {
                    removeWeaknessPersistentData(heldItem);
                    championsManager.getEffects().removeEffect(player, EffectTypes.WEAKNESS);
                }
            }
        }
    }

    private void reapplyWeakness(Player player, ItemStack item) {
        long expirationTime = getWeaknessExpirationTime(item);
        double remainingDuration = (expirationTime - System.currentTimeMillis()) / 1000.0;
        championsManager.getEffects().addEffect(player, EffectTypes.WEAKNESS, getName(), weaknessStrength, (long) (remainingDuration * 1000L));
    }

    private void removeWeakness(Player player) {
        championsManager.getEffects().removeEffect(player, EffectTypes.WEAKNESS);
    }

    private boolean hasWeaknessPersistentData(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (!item.hasItemMeta()) return false;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return container.has(WEAKNESS_KEY, PersistentDataType.LONG);
    }

    private long getWeaknessExpirationTime(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(WEAKNESS_KEY, PersistentDataType.LONG)) {
            return container.get(WEAKNESS_KEY, PersistentDataType.LONG);
        }
        return 0;
    }

    private void applyWeakness(Player player, ItemStack item, double duration) {
        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        long expirationTime = System.currentTimeMillis() + (long) (duration * 1000);

        container.set(WEAKNESS_KEY, PersistentDataType.LONG, expirationTime);
        item.setItemMeta(meta);

        championsManager.getEffects().addEffect(player, EffectTypes.WEAKNESS, getName(), weaknessStrength, (long) (duration * 1000L));
    }

    private void removeWeaknessPersistentData(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(WEAKNESS_KEY);
        item.setItemMeta(meta);
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public void loadSkillConfig() {
        weaknessDuration = getConfig("weaknessDuration", 2.0, Double.class);
        weaknessDurationIncreasePerLevel = getConfig("weaknessDurationIncreasePerLevel", 1.0, Double.class);
        weaknessStrength = getConfig("basePushBack", 2, Integer.class);
        internalCooldown = getConfig("internalCooldown", 5.0, Double.class);
        internalCooldownDecreasePerLevel = getConfig("internalCooldownDecreasePerLevel", 1.0, Double.class);
    }
}
