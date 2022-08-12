package me.mykindos.betterpvp.clans.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.builds.menus.events.SkillDequipEvent;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.*;
import org.bukkit.ChatColor;
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

    @Inject
    public Wreath(Clans clans, ChampionsManager championsManager) {
        super(clans, championsManager);
    }

    @Override
    public String getName() {
        return "Wreath";
    }

    @Override
    public String[] getDescription(int level) {
        return new String[]{
                "Right click with a sword to prepare.",
                "",
                "Your next 3 attacks will release a barrage of teeth",
                "that deal " + ChatColor.GREEN + String.format("%.2f", (2 + (level / 1.5))) + ChatColor.GRAY + " damage and slow their target.",
                "",
                "Recharge: " + ChatColor.GREEN + getCooldown(level)
        };
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
    public void onSwing(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().isLeftClick()) return;
        if (!UtilPlayer.isHoldingItem(event.getPlayer(), SkillWeapons.SWORDS)) return;
        if (!actives.containsKey(event.getPlayer())) return;

        Player player = event.getPlayer();
        int stacks = actives.get(event.getPlayer());
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
                if (championsManager.getCooldowns().add(player, getName(), getCooldown(level), showCooldownFinished())) {

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
                    for (var data : UtilEntity.getNearbyEnemies(player, fangs.getLocation(), 1.5)) {
                        LivingEntity ent = data.get();
                        CustomDamageEvent dmg = new CustomDamageEvent(ent, player, null, EntityDamageEvent.DamageCause.CUSTOM, 2 + (getLevel(player) / 1.5), false, getName());
                        UtilDamage.doCustomDamage(dmg);
                        ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1));
                    }

                }

            }.runTaskTimer(clans, 0, 1);

            new BukkitRunnable() {

                @Override
                public void run() {
                    runnable.cancel();

                }

            }.runTaskLater(clans, 60);
        } else {
            actives.remove(player);
        }

    }


    @Override
    public double getCooldown(int level) {
        return cooldown - (level * 2);
    }

    @Override
    public void activate(Player player, int level) {
        actives.put(player, 3);
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
}
