package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.*;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Singleton
@BPvPListener
public class WolfsPounce extends ChannelSkill implements InteractSkill, CooldownSkill {

    // Percentage (0 -> 1)
    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, PounceData> pounceData = new WeakHashMap<>();

    private double baseCharge;
    private double baseDamage;

    private double slowDuration;

    @Inject
    public WolfsPounce(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Wolfs Pounce";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[] {
                "Hold right click with a Sword to channel",
                "",
                "Charges <val>" + getChargePerSecond(level) + "%</val> per second",
                "",
                "Release right click to pounce forward",
                "in the direction you are looking",
                "",
                "Colliding with another player mid-air",
                "will deal up to <val>" + getDamage(level) + "</val> damage and apply",
                "<effect>Slowness II</effect> for <stat>" + slowDuration + "</stat> seconds",
                "",
                "Taking damage cancels charge",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public boolean canUse(Player p) {
        if (!UtilBlock.isGrounded(p)) {
            UtilMessage.simpleMessage(p, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
            return false;
        }
        return true;
    }

    private double getDamage(int level) {
        return baseDamage + (level - 1);
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (10 * (level - 1)); // Increment of 10% per level
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - (level - 1d);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        baseCharge = getConfig("baseCharge", 40.0, Double.class);
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        slowDuration = getConfig("slowDuration", 3.0, Double.class);
    }

    @Override
    public void activate(Player player, int level) {
        charging.put(player, new ChargeData(level));
    }

    private void pounce(Player player, double charge, int level) {
        charging.remove(player); // Remove their charge

        // Velocity
        final double strength = 0.4 + (1.4 * charge);
        UtilVelocity.velocity(player, strength, 0.2, 0.4 + (0.9 * charge), true);

        // Pounce log
        pounceData.put(player, new PounceData(charge, level));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, 0.8f + (1.2f * (float) charge));

        // Cooldown
        championsManager.getCooldowns().add(player, getName(), getCooldown(level), showCooldownFinished());
    }

    private void collide(Player damager, LivingEntity damagee, PounceData pounceData) {
        final int level = pounceData.getLevel();
        double damage = getDamage(level) * pounceData.getCharge();

        // Effects & Damage
        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
        damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) slowDuration * 20, 1));

        // Cues
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", damagee.getName(), getName(), level);
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", damager.getName(), getName(), level);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5f, 0.5f);
    }

    private void showCharge(Player player, ChargeData charge) {
        // Action bar
        int green = (int) Math.round(charge.getCharge() * 15);
        int red = 15 - green;

        String msg = "<green><bold>" + "\u258B".repeat(Math.max(0, green)) + "<red><bold>" + "\u258B".repeat(Math.max(0, red));
        final Component bar = MiniMessage.miniMessage().deserialize(msg);
        player.sendActionBar(bar);

        // Sound
        if (!UtilTime.elapsed(charge.getLastSound(), 150)) {
            return;
        }

        player.playSound(player.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f + (0.5f * (float) charge.getCharge()));
        charge.setLastSound(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void cancelCooldown(PlayerUseSkillEvent event) {
        if (event.getSkill() == this && charging.containsKey(event.getPlayer())) {
            event.setCancelled(true); // Cancel cooldown or ability use if they're charging to allow them to release
        }
    }

    @EventHandler
    public void onDamageReceived(CustomDamageEvent event) {
        if (!(event.getDamagee() instanceof Player player)) return;

        if (hasSkill(player) && charging.containsKey(player)) {
            charging.remove(player);
            // Cues
            UtilMessage.simpleMessage(player, getClassType().getName(), "<alt>%s</alt> was interrupted.", getName());
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 0.6f, 1.2f);
        }
    }

    @UpdateEvent
    public void checkCollide() {
        pounceData.entrySet().removeIf(entry -> entry.getValue().hasExpired());

        // Collision check
        final Iterator<Player> pounceGamers = pounceData.keySet().iterator();
        while (pounceGamers.hasNext()) {
            final Player player = pounceGamers.next();
            final List<LivingEntity> enemies = UtilEntity.getNearbyEnemies(player, player.getLocation(), 2);
            if (enemies.isEmpty()) {
                continue; // skip if no collision
            }

            // collide with first
            final PounceData data = pounceData.get(player);
            final LivingEntity enemy = enemies.get(0);
            pounceGamers.remove(); // Remove them because they collided
            collide(player, enemy, data);
        }
    }

    @UpdateEvent
    public void updateCharge() {
        // Charge check
        Iterator<Player> iterator = charging.keySet().iterator();
        while (iterator.hasNext()) {
            Player player = iterator.next();
            ChargeData charge = charging.get(player);
            if (player != null) {
                int level = getLevel(player);

                // Remove if they no longer have the skill
                if (level <= 0) {
                    iterator.remove();
                    continue;
                }

                // Check if they still are blocking and charge
                if (player.isHandRaised()) {
                    // Cancel cooldown to make it only start after we call #pounce
                    championsManager.getCooldowns().removeCooldown(player, getName(), true);

                    // Check for sword hold status
                    if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                        iterator.remove(); // Otherwise, remove
                    }

                    if (!UtilBlock.isGrounded(player)) {
                        if (UtilTime.elapsed(charge.getLastMessage(), 250)) {
                            UtilMessage.simpleMessage(player, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
                            charge.setLastMessage(System.currentTimeMillis());
                        }
                        continue;
                    }

                    charge.tick();

                    // Cues
                    showCharge(player, charge);
                    continue;
                }

                if (UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                    // If they're not blocking and still holding their sword, pounce
                    pounce(player, charge.getCharge(), level);
                }
            }
        }
    }

    @Data
    private static class PounceData {

        private final long pounceTime = System.currentTimeMillis();
        private final double charge; // 0 -> 1
        private final int level;

        public boolean hasExpired() {
            return System.currentTimeMillis() - pounceTime > 1000;
        }

    }

    @Data
    private class ChargeData {

        private long lastSound = 0;
        private long lastMessage = 0;
        private double charge = 0; // 0 -> 1
        private final int level;

        public void tick() {
            // Divide over 100 to get multiplication factor since it's in 100% scale for display
            final double chargeToGive = getChargePerSecond(level) / 100;
            this.charge = Math.min(1, this.charge + (chargeToGive / 20));
        }

    }
}
