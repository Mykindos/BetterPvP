package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Data;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolfsPounce extends ChannelSkill implements InteractSkill, CooldownSkill {

    // Percentage (0 -> 1)
    private final WeakHashMap<Player, ChargeData> charging = new WeakHashMap<>();
    private final WeakHashMap<Player, PounceData> pounceData = new WeakHashMap<>();

    // Action bar
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();
        if (player == null || !charging.containsKey(player) || !isHolding(player)) {
            return null; // Skip if not online or not charging
        }

        final ChargeData charge = charging.get(player);
        ProgressBar progressBar = ProgressBar.withProgress((float) charge.getCharge());
        return progressBar.build();
    });

    private double baseCharge;

    private double chargeIncreasePerLevel;

    private double baseDamage;

    private double damageIncreasePerLevel;

    private double baseSlowDuration;
    
    private double slowDurationIncreasePerLevel;

    private int slowStrength;

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
                "<effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect> for <stat>" + getSlowDuration(level) + "</stat> seconds",
                "",
                "Taking damage cancels charge",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + level * slowDurationIncreasePerLevel;
    }

    @Override
    public void trackPlayer(Player player) {
        // Action bar
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().add(900, actionBarComponent);
    }

    @Override
    public void invalidatePlayer(Player player) {
        // Action bar
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public boolean canUse(Player p) {
        if (!UtilBlock.isGrounded(p)) {
            UtilMessage.simpleMessage(p, getClassType().getName(), "You cannot use <alt>" + getName() + "</alt> in the air.");
            return false;
        }
        return super.canUse(p);
    }

    private double getDamage(int level) {
        return baseDamage + (level - 1) * damageIncreasePerLevel;
    }

    private double getChargePerSecond(int level) {
        return baseCharge + (chargeIncreasePerLevel * (level - 1)); // Increment of 10% per level
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
        return cooldown - (level - 1d) * cooldownDecreasePerLevel;
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseCharge = getConfig("baseCharge", 40.0, Double.class);
        chargeIncreasePerLevel = getConfig("chargeIncreasePerLevel", 10.0, Double.class);
        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseSlowDuration = getConfig("slowDuration", 3.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);

        slowStrength = getConfig("slowStrength", 1, Integer.class);
    }

    @Override
    public void activate(Player player, int level) {
        final ChargeData chargeData = new ChargeData(level);
        charging.put(player, chargeData);
    }

    @Override
    public boolean shouldDisplayActionBar(Gamer gamer) {
        return false;
    }

    private void pounce(Player player, double charge, int level) {
        charging.remove(player); // Remove their charge

        // Velocity
        final double strength = 0.4 + (1.4 * charge);
        UtilVelocity.velocity(player, strength, 0.2, 0.4 + (0.9 * charge), true);

        // Pounce log
        pounceData.put(player, new PounceData(charge, level));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1f, 0.8f + (1.2f * (float) charge));

        // Cooldown & Action Bar disabling
        championsManager.getCooldowns().use(player,
                getName(),
                getCooldown(level),
                true,
                true,
                false,
                gmr -> gmr.getPlayer() != null && isHolding(gmr.getPlayer()));
    }

    private void collide(Player damager, LivingEntity damagee, PounceData pounceData) {
        final int level = pounceData.getLevel();
        double damage = getDamage(level) * pounceData.getCharge();

        // Effects & Damage
        UtilDamage.doCustomDamage(new CustomDamageEvent(damagee, damager, null, EntityDamageEvent.DamageCause.CUSTOM, damage, true, getName()));
        damagee.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) baseSlowDuration * 20, 1));

        // Cues
        UtilMessage.simpleMessage(damager, getClassType().getName(), "You hit <alt2>%s</alt2> with <alt>%s %s</alt>.", damagee.getName(), getName(), level);
        UtilMessage.simpleMessage(damagee, getClassType().getName(), "<alt2>%s</alt2> hit you with <alt>%s %s</alt>.", damager.getName(), getName(), level);
        damager.getWorld().playSound(damager.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5f, 0.5f);
    }

    private void showCharge(Player player, ChargeData charge) {
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
        if(event.isCancelled()) return;
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
                Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                int level = getLevel(player);

                // Remove if they no longer have the skill
                if (level <= 0) {
                    iterator.remove();
                    continue;
                }

                // Check if they still are blocking and charge
                if (gamer.isHoldingRightClick()) {
                    // Cancel cooldown to make it only start after we call #pounce
                    championsManager.getCooldowns().removeCooldown(player, getName(), true);

                    // Check for sword hold status
                    if (!isHolding(player)) {
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

                if (isHolding(player)) {
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
            final double chargeToGive = getChargePerSecond(level)/100;
            this.charge = Math.min(1, this.charge + (chargeToGive / 20));
        }

    }
}
