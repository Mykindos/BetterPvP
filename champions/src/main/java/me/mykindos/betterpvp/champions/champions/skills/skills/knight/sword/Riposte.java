package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.knight.data.RiposteData;
import me.mykindos.betterpvp.champions.champions.skills.types.ChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DefensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


@Singleton
@BPvPListener
public class Riposte extends ChannelSkill implements CooldownSkill, InteractSkill, OffensiveSkill, DamageSkill, HealthSkill, DefensiveSkill {

    private final HashMap<UUID, Long> handRaisedTime = new HashMap<>();
    private final HashMap<UUID, RiposteData> riposteData = new HashMap<>();

    private double baseDuration;

    private double durationIncreasePerLevel;

    private double baseBonusDamageDuration;

    private double bonusDamageDurationIncreasePerLevel;

    private double baseBonusDamage;

    private double bonusDamageIncreasePerLevel;

    private double baseHealing;

    private double healingIncreasePerLevel;

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
                "If an enemy hits you within " + getValueString(this::getDuration, level) + " seconds,",
                "You will heal " + getValueString(this::getHealing, level) + " health and your next",
                "attack will deal " + getValueString(this::getBonusDamage, level) + " extra damage",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level)
        };
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
    }

    public double getBonusDamage(int level) {
        return baseBonusDamage + ((level - 1) * bonusDamageIncreasePerLevel);
    }

    public double getBonusDamageDuration(int level) {
        return baseBonusDamageDuration + ((level - 1) * bonusDamageDurationIncreasePerLevel);
    }

    public double getHealing(int level) {
        return baseHealing + ((level - 1) * healingIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onRiposte(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
        if (!gamer.isHoldingRightClick()) return;
        LivingEntity ent = event.getDamager();

        int level = getLevel(player);
        if (level > 0) {
            event.setKnockback(false);
            event.setDamage(0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2.0f, 1.3f);

            double newHealth = getHealing(level);
            UtilPlayer.health(player, newHealth);

            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s %d<gray>.", getName(), level);
            if (ent instanceof Player target) {
                UtilMessage.simpleMessage(target, getClassType().getName(), "<yellow>%s<gray> used <green>%s %d</green>", player.getName(), getName(), level);
            }

            active.remove(player.getUniqueId());
            handRaisedTime.remove(player.getUniqueId());

            riposteData.put(player.getUniqueId(), new RiposteData(System.currentTimeMillis(), getBonusDamage(level)));

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

        Particle.LARGE_SMOKE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();

        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                Gamer gamer = championsManager.getClientManager().search().online(player).getGamer();
                if (gamer.isHoldingRightClick() && !handRaisedTime.containsKey(player.getUniqueId())) {
                    handRaisedTime.put(player.getUniqueId(), System.currentTimeMillis());
                    continue;
                }

                if (riposteData.containsKey(player.getUniqueId())) continue;

                if (!gamer.isHoldingRightClick() && handRaisedTime.containsKey(player.getUniqueId())) {
                    failRiposte(player);
                    it.remove();
                    continue;
                }

                int level = getLevel(player);

                if (gamer.isHoldingRightClick() && UtilTime.elapsed(handRaisedTime.getOrDefault(player.getUniqueId(), 0L), (long) getDuration(level) * 1000L)) {
                    failRiposte(player);
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
    }

    private void failRiposte(Player player) {
        handRaisedTime.remove(player.getUniqueId());
        UtilMessage.simpleMessage(player, getClassType().getName(),"You failed <green>%s %d</green>", getName(), getLevel(player));
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
            int level = getLevel(player);
            if (UtilTime.elapsed(riposteData.getBoostedAttackTime(), (long) (getBonusDamageDuration(level) * 1000))) {
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
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 0.75, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);

        baseBonusDamageDuration = getConfig("baseBonusDamageDuration", 2.0, Double.class);
        bonusDamageDurationIncreasePerLevel = getConfig("bonusDamageDurationIncreasePerLevel", 0.0, Double.class);

        baseBonusDamage = getConfig("baseBonusDamage", 1.0, Double.class);
        bonusDamageIncreasePerLevel = getConfig("bonusDamageIncreasePerLevel", 0.5, Double.class);

        baseHealing = getConfig("baseHealing", 1.0, Double.class);
        healingIncreasePerLevel = getConfig("healingIncreasePerLevel", 1.0, Double.class);
    }
}
