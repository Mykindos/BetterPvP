package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Evade extends ChannelSkill implements InteractSkill, CooldownSkill, EnergySkill {

    private final WeakHashMap<Player, Long> gap = new WeakHashMap<>();
    private final WeakHashMap<Player, Long> delay = new WeakHashMap<>();

    @Inject
    public Evade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Evade";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to channel",
                "",
                "If a player hits you while evading,",
                "you will teleport behind the attacker",
                "",
                "Hold Crouch while Evading to teleport backwards",
                "",
                "<stat>" + getCooldown(level) + "</stat> second internal cooldown",
                "",
                "Energy / Second: <val>" + (10 * getEnergy(level))};
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }


    @EventHandler
    public void onEvade(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        LivingEntity ent = event.getDamager();


        if (hasSkill(player)) {

            if (!delay.containsKey(player)) {
                delay.put(player, 0L);
            }

            event.setKnockback(false);
            event.cancel("Skill Evade");
            if (UtilTime.elapsed(delay.get(player), 500)) {
                for (int i = 0; i < 3; i++) {
                    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 5);
                }
                Location target;
                if (player.isSneaking()) {
                    target = findLocationBack(ent, player);
                } else {
                    target = findLocationBehind(ent, player);
                }

                if (target != null) {
                    player.teleport(target);
                }

                UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());
                if (ent instanceof Player temp) {
                    UtilMessage.simpleMessage(temp, getClassType().getName(), "<yellow>%s<gray> used evade!", player.getName());
                }

                delay.put(player, System.currentTimeMillis());
            }

        }

    }

    @EventHandler
    public void onCustomVelcity(CustomEntityVelocityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;

        if (hasSkill(player)) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent(delay = 100)
    public void onUpdateEffect() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                if (player.isHandRaised()) {
                    player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, 7);
                }
            } else {
                it.remove();
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                int level = getLevel(player);
                if (level > 0) {
                    if (player.isHandRaised()) {
                        if (!championsManager.getEnergy().use(player, getName(), getEnergy(getLevel(player)), true)) {
                            it.remove();
                        } else if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                            it.remove();
                        } else if(UtilBlock.isInLiquid(player)){
                            it.remove();
                        } else if(championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                            it.remove();
                        }else if(championsManager.getEffects().hasEffect(player, EffectType.STUN)) {
                            it.remove();
                        }
                    } else {
                        if (gap.containsKey(player)) {
                            if (UtilTime.elapsed(gap.get(player), 250)) {
                                gap.remove(player);
                                it.remove();

                            }
                        }

                    }
                } else {
                    it.remove();
                }
            }

        }

    }


    @EventHandler
    public void onDamage(CustomDamageEvent e) {
        if (e.getDamager() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                e.cancel("Skill: Evade");
            }
        }
    }

    private Location findLocationBehind(LivingEntity damager, Player damagee) {
        double curMult = 0.0D;
        double maxMult = 1.5D;

        double rate = 0.1D;

        Location lastValid = damager.getLocation();
        Location lastValid2 = damagee.getLocation();
        while (curMult <= maxMult) {
            Vector vec = UtilVelocity.getTrajectory(damagee, damager).multiply(curMult);
            Location loc = damagee.getLocation().add(vec);


            if (loc.getBlock().getType().name().contains("DOOR") || loc.getBlock().getType().name().contains("GATE")) {

                return lastValid2;
            }



            if ((!UtilBlock.airFoliage(loc.getBlock())) || (!UtilBlock.airFoliage(loc.getBlock().getRelative(BlockFace.UP)))) {

                Block b2 = loc.add(0, 1, 0).getBlock();
                if (UtilBlock.airFoliage(b2) && UtilBlock.airFoliage(b2.getRelative(BlockFace.UP))) {

                    break;
                }

                return lastValid2;
            }


            curMult += rate;
        }

        curMult = 0.0D;

        while (curMult <= maxMult) {
            Vector vec = UtilVelocity.getTrajectory(damager, damagee).multiply(curMult);
            Location loc = damager.getLocation().subtract(vec);

            if (loc.getBlock().getType().name().contains("DOOR") || loc.getBlock().getType().name().contains("GATE")) {
                UtilVelocity.velocity(damagee, UtilVelocity.getTrajectory(damagee, damager), 0.3, false, 0, 0.1, 0.2, false);
                return lastValid;
            }

            if ((!UtilBlock.airFoliage(loc.getBlock())) || (!UtilBlock.airFoliage(loc.getBlock().getRelative(BlockFace.UP)))) {
                return lastValid;
            }
            lastValid = loc;

            curMult += rate;
        }

        return lastValid;
    }

    private Location findLocationBack(LivingEntity damager, Player damagee) {
        double curMult = 0.0D;
        double maxMult = 3.0D;

        double rate = 0.1D;

        Location lastValid = damagee.getLocation();

        while (curMult <= maxMult) {

            Vector vec = UtilVelocity.getTrajectory(damager, damagee).multiply(curMult);
            Location loc = damagee.getLocation().add(vec);

            if (loc.getBlock().getType().name().contains("DOOR") || loc.getBlock().getType().name().contains("GATE")) {
                UtilVelocity.velocity(damagee, UtilVelocity.getTrajectory(damagee, damager), 0.3, false, 0, 0.1, 0.2, false);
                return lastValid;
            }

            if ((!UtilBlock.airFoliage(loc.getBlock())) || (!UtilBlock.airFoliage(loc.getBlock().getRelative(BlockFace.UP)))) {
                return lastValid;
            }

            lastValid = loc;
            curMult += rate;
        }

        return lastValid;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown;
    }

    @Override
    public float getEnergy(int level) {

        return (float) (energy - ((level - 1)));
    }

    @Override
    public boolean canUse(Player player) {
        return !active.contains(player.getUniqueId());
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        gap.put(player, System.currentTimeMillis());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
