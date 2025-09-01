package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public abstract class PrepareArrowSkill extends PrepareSkill implements CooldownSkill {

    protected final Set<Arrow> arrows = Collections.newSetFromMap(new WeakHashMap<>());

    public PrepareArrowSkill(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void onHit(DamageEvent event) {
        if (event.isCancelled() || !event.isDamageeLiving()) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!arrows.contains(arrow)) return;

        int level = getLevel(damager);
        if (level > 0) {

            onHit(damager, event.getLivingDamagee(), level);
            arrows.remove(arrow);
            event.addReason(getName());

        }

    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShoot(EntityShootBowEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (!(event.getProjectile() instanceof Arrow arrow)) return;

        int level = getLevel(player);
        if (level > 0) {
            if(championsManager.getCooldowns().use(player, getName(), getCooldown(level), showCooldownFinished(), true, isCancellable(), this::shouldDisplayActionBar)) {
                processEntityShootBowEvent(event, player, level, arrow);
                active.remove(player.getUniqueId());
                onFire(player);
            }
        }
    }

    public void processEntityShootBowEvent(EntityShootBowEvent event, Player player, int level, Arrow arrow) {
        arrows.add(arrow);
    }

    @UpdateEvent
    public void updateParticle() {
        Iterator<Arrow> it = arrows.iterator();
        while (it.hasNext()) {
            Arrow next = it.next();
            if (next == null || next.isOnGround()) {
                it.remove();
            } else if (next.isDead()) {
                it.remove();
            } else {
                Location loc = next.getLocation().add(new Vector(0, 0.25, 0));
                displayTrail(loc);

            }
        }
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
            }

        }

    }

    @UpdateEvent(delay = 1000)
    public void update() {
        arrows.removeIf(Entity::isDead);

    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public abstract void onHit(Player damager, LivingEntity target, int level);

    public abstract void displayTrail(Location location);

    public void onFire(Player shooter) {
        // Overridable - Not abstract to avoid breaking existing skills
    }

}
