package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.comphenix.protocol.PacketType;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Wreath extends PrepareSkill implements CooldownSkill {

    private final WeakHashMap<Player, Integer> actives = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> cooldowns = new WeakHashMap<>();

    private int baseNumAttacks;
    private int numAttacksIncreasePerLevel;
    private double baseSlowDuration;
    private double slowDurationIncreasePerLevel;
    private double baseDamage;
    private double damageIncreasePerLevel;
    private int slowStrength;
    private double healthPerEnemyHit;

    @Inject
    public Wreath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wreath";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a Sword to prepare",
                "",
                "Your next <stat>" + getNumAttacks(level) + "</stat> attacks will release a barrage of",
                "teeth that deal <val>" + String.format("%.2f", getDamage(level)) + "</val> damage and apply <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength + 1) + "</effect>",
                "to their target for <stat>" + getSlowDuration(level) + "</stat> seconds.",
                "",
                "For each enemy hit, restore <val>" + healthPerEnemyHit + "</val> health.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    public int getNumAttacks(int level) {
        return baseNumAttacks + level * numAttacksIncreasePerLevel;
    }

    public double getDamage(int level) {
        return baseDamage + level * damageIncreasePerLevel;
    }

    public double getSlowDuration(int level) {
        return baseSlowDuration + level * slowDurationIncreasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler
    public void onDequip(SkillDequipEvent event) {
        if (event.getSkill().equals(this)) {
            actives.remove(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        actives.remove(event.getEntity());
    }

    @EventHandler
    public void onDamage(PreCustomDamageEvent event) {
        if(!(event.getCustomDamageEvent().getDamager() instanceof Player player)) return;
        if(event.getCustomDamageEvent().getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!isHolding(player)) return;
        if (!actives.containsKey(player)) return;

        processPlayerAction(player);
    }

    @EventHandler
    public void onSwing(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isLeftClick()) return;
        if (!isHolding(event.getPlayer())) return;
        if (!actives.containsKey(event.getPlayer())) return;

        Player player = event.getPlayer();
        processPlayerAction(player);

    }

    private void processPlayerAction(Player player) {
        int stacks = actives.get(player);
        if (stacks > 0) {
            int level = getLevel(player);
            if (level <= 0) return;
            if (cooldowns.containsKey(player)) {
                if (cooldowns.get(player) - System.currentTimeMillis() > 0) {
                    return;
                }
            }

            cooldowns.put(player, System.currentTimeMillis() + 600);
            actives.put(player, stacks - 1);

            if (actives.get(player) == 0) {
                championsManager.getCooldowns().removeCooldown(player, getName(), true);
                if (championsManager.getCooldowns().use(player, getName(), getCooldown(level), showCooldownFinished())) {

                }
            }

            final Location startPos = player.getLocation().clone();
            final Vector vector = player.getLocation().clone().getDirection().normalize().multiply(1);
            vector.setY(0);
            final Location loc = player.getLocation().subtract(0, 1, 0).add(vector);

            final BukkitTask runnable = new BukkitRunnable() {

                @Override
                public void run() {
                    loc.add(vector);
                    if ((!UtilBlock.airFoliage(loc.getBlock()))
                            && UtilBlock.solid(loc.getBlock())) {

                        loc.add(0.0D, 1.0D, 0.0D);
                        if ((!UtilBlock.airFoliage(loc.getBlock()))
                                && UtilBlock.solid(loc.getBlock())) {

                            cancel();
                            return;
                        }

                    }

                    if (loc.getBlock().getType().name().contains("DOOR")) {
                        cancel();
                        return;
                    }

                    if ((loc.clone().add(0.0D, -1.0D, 0.0D).getBlock().getType() == Material.AIR)) {
                        loc.add(0.0D, -1.0D, 0.0D);
                    }

                    if (loc.distance(startPos) > 20) {
                        cancel();
                    }

                    EvokerFangs fangs = (EvokerFangs) player.getWorld().spawnEntity(loc, EntityType.EVOKER_FANGS);
                    for (LivingEntity target : UtilEntity.getNearbyEnemies(player, fangs.getLocation(), 1.5)) {
                        CustomDamageEvent dmg = new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(level), false, getName());
                        UtilDamage.doCustomDamage(dmg);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (getSlowDuration(level) * 20), slowStrength));
                        UtilPlayer.health(player, healthPerEnemyHit);
                    }

                }

            }.runTaskTimer(champions, 0, 1);

            UtilServer.runTaskLater(champions, runnable::cancel, 60);

        } else {
            actives.remove(player);
        }
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        actives.put(player, getNumAttacks(level));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 1.8f);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Override
    public boolean canUse(Player player) {
        if (actives.containsKey(player)) {
            int stacks = actives.get(player);
            if (stacks > 0) {
                UtilMessage.simpleMessage(player, getClassType().getName(), "<green>%s<gray> is already active with <green>%d<gray> stacks remaining",
                        getName(), stacks);
                return false;
            }
        }

        return true;
    }

    @Override
    public void loadSkillConfig() {
        baseNumAttacks = getConfig("baseNumAttacks", 3, Integer.class);
        numAttacksIncreasePerLevel = getConfig("numAttacksIncreasePerLevel", 0, Integer.class);

        baseSlowDuration = getConfig("baseSlowDuration", 2.0, Double.class);
        slowDurationIncreasePerLevel = getConfig("slowDurationIncreasePerLevel", 0.0, Double.class);

        baseDamage = getConfig("baseDamage", 2.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.66, Double.class);

        healthPerEnemyHit = getConfig("healthPerEnemyHit", 1.0, Double.class);

        slowStrength = getConfig("slowStrength", 1, Integer.class);
    }
}
