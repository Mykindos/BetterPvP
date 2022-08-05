package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.bow;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.clans.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.clans.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Singleton
@BPvPListener
public class SilencingArrow extends PrepareSkill implements CooldownSkill, Listener {

    private final List<Arrow> arrows = new ArrayList<>();

    @Inject
    public SilencingArrow(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Silencing Arrow";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your next arrow will silence your",
                "target for " + ChatColor.GREEN + (3 + level) + ChatColor.GRAY + " seconds.",
                "Making them unable to use any active skills",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)

        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.BOW;
    }

    @UpdateEvent(delay = 250)
    public void onCheckCancel() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                it.remove();
                continue;
            }

            int level = getLevel(player);
            if (level <= 0) {
                it.remove();
                continue;
            }

            if (!UtilPlayer.isHoldingItem(player, SkillWeapons.BOWS)) {
                Cooldown cooldown = championsManager.getCooldowns().getAbilityRecharge(player, getName());
                if (cooldown != null) {
                    if (cooldown.isCancellable()) {
                        championsManager.getCooldowns().removeCooldown(player, getName(), true);
                        UtilMessage.message(player, getClassType().getName(), ChatColor.GREEN + getName() + " " + level + ChatColor.GRAY + " was cancelled.");
                        it.remove();
                    }
                }

            }


        }

    }

    @Override
    public double getCooldown(int level) {
        return getSkillConfig().getCooldown() - ((level - 1) * 0.5);
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        int level = getLevel(player);
        if (level > 0) {
            if (event.getProjectile() instanceof Arrow) {
                if (active.contains(player.getUniqueId())) {
                    arrows.add((Arrow) event.getProjectile());
                    active.remove(player.getUniqueId());

                    championsManager.getCooldowns().removeCooldown(player, getName(), true);
                    championsManager.getCooldowns().add(player, getName(), getCooldown(getLevel(player)), showCooldownFinished(), true, false);

                }
            }


        }
    }

    @UpdateEvent
    public void updateParticle() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow next = it.next();
            if (next == null) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location loc = next.getLocation().add(new Vector(0, 0.25, 0));
                Particle.REDSTONE.builder().location(loc).color(125, 0, 125).count(3).extra(0).receivers(60, true).spawn();
            }
        }
    }

    @EventHandler
    public void onHit(CustomDamageEvent e) {
        if (!(e.getProjectile() instanceof Arrow)) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!(e.getDamagee() instanceof Player damagee)) return;

        int level = getLevel(damager);
        if (level <= 0) return;
        if (arrows.contains((Arrow) e.getProjectile())) {

            championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (3 + level * 1000L));
            e.setReason(getName());
            if (championsManager.getEffects().hasEffect(damagee, EffectType.IMMUNETOEFFECTS)) {
                UtilMessage.message(damager, getClassType().getName(), ChatColor.GREEN + damagee.getName() + ChatColor.GRAY + " is immune to your silence!");
            }
            arrows.remove((Arrow) e.getProjectile());

        }
    }

    @UpdateEvent(delay = 1000)
    public void update() {
        arrows.removeIf(Entity::isDead);
    }

    @Override
    public void activate(Player player, int level) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 2.5F, 2.0F);
            active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.LEFT_CLICK;
    }
}
