package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.RiposteData;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;


@Singleton
@BPvPListener
public class Riposte extends ChannelSkill implements CooldownSkill, InteractSkill {

    private final WeakHashMap<Player, Long> handRaisedTime = new WeakHashMap<>();
    private final HashMap<UUID, RiposteData> riposteData = new HashMap<>();

    public double duration;
    public double bonusDamage;
    public double healing;

    @Inject
    public Riposte(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Riposte";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Hold right click with a Sword to activate",
                "",
                "If an enemy hits you within <stat>" + duration + "</stat> seconds,",
                "You will heal <val>" + (healing + (level - 1)) + "</val> health and your next",
                "attack will deal <val>" + (bonusDamage + (level - 1)) + "</val> extra damage",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler
    public void onRiposte(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        if (!player.isHandRaised()) return;
        LivingEntity ent = event.getDamager();

        int level = getLevel(player);
        if (level > 0) {
            event.setKnockback(false);
            event.setDamage(0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2.0f, 1.3f);

            double newHealth = player.getHealth() + (healing + (level - 1));
            UtilPlayer.health(player, newHealth);

            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());
            if (ent instanceof Player target) {
                UtilMessage.simpleMessage(target, getClassType().getName(), "<yellow>%s<gray> used riposte!", player.getName());
            }

            active.remove(player.getUniqueId());
            handRaisedTime.remove(player);

            riposteData.put(player.getUniqueId(), new RiposteData(System.currentTimeMillis(), bonusDamage + (level - 1)));

        }
    }

    @EventHandler
    public void onAttack(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!riposteData.containsKey(player.getUniqueId())) return;

        RiposteData data = riposteData.remove(player.getUniqueId());
        event.setDamage(event.getDamage() + data.getBoostedDamage());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 1.0f);
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);

        Particle.SMOKE_LARGE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();
    }

    @UpdateEvent(delay = 100)
    public void onUpdateEffect() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                if (player.isHandRaised()) {
                    player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.IRON_BLOCK);
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
                if (player.isHandRaised() && !handRaisedTime.containsKey(player)) {
                    handRaisedTime.put(player, System.currentTimeMillis());
                    continue;
                }

                if (riposteData.containsKey(player.getUniqueId())) continue;

                if (!player.isHandRaised() && handRaisedTime.containsKey(player)) {
                    failRiposte(player);
                    it.remove();
                    continue;
                }

                if (player.isHandRaised() && handRaisedTime.containsKey(player) && UtilTime.elapsed(handRaisedTime.get(player), (long) duration * 1000L)) {
                    failRiposte(player);
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    private void failRiposte(Player player) {
        handRaisedTime.remove(player);
        UtilMessage.message(player, getClassType().getName(), "Your Riposte failed.");
        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
    }

    @UpdateEvent
    public void processBoostedPlayers() {
        Iterator<Map.Entry<UUID, RiposteData>> boostedIterator = riposteData.entrySet().iterator();
        while (boostedIterator.hasNext()) {
            Map.Entry<UUID, RiposteData> next = boostedIterator.next();

            UUID uuid = next.getKey();
            RiposteData riposteData = next.getValue();

            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                boostedIterator.remove();
                continue;
            }

            if (UtilTime.elapsed(riposteData.getBoostedAttackTime(), 2000)) {
                UtilMessage.message(player, getClassType().getName(), "You lost your boosted attack.");
                player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                boostedIterator.remove();
            }

        }

    }

    @EventHandler
    public void onRiposteDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        riposteData.remove(player.getUniqueId());
        active.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        active.remove(player.getUniqueId());
        riposteData.remove(player.getUniqueId());
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 1.5);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        duration = getConfig("duration", 0.75, Double.class);
        bonusDamage = getConfig("bonusDamage", 1.0, Double.class);
        healing = getConfig("healing", 1.0, Double.class);
    }
}
