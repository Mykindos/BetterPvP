package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;

import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.effects.events.EffectClearEvent;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class SmokeBomb extends Skill implements ToggleSkill, CooldownSkill, Listener {

    private double baseDuration;

    private double blindDuration;

    private double blindRadius;

    private final WeakHashMap<Player, Integer> smoked = new WeakHashMap<>();

    @Inject
    public SmokeBomb(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @EventHandler
    public void preventSmokeDamage(CustomDamageEvent event) {

        if (!(event.getDamagee() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
        if (!championsManager.getEffects().hasEffect(player, EffectType.INVISIBILITY)) return;
        if (!hasSkill(player)) return;

        event.cancel("Can't take damage during smoke bomb");
    }


    @UpdateEvent(delay = 500)
    public void onUpdate() {
        Iterator<Entry<Player, Integer>> it = smoked.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Player, Integer> next = it.next();

            Optional<Role> roleOptional = championsManager.getRoles().getObject(next.getKey().getUniqueId());
            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();

                if (role == Role.ASSASSIN) {
                    if (next.getValue() > 0) {
                        for (int i = 0; i < 5; i++) {
                            Particle.SMOKE_LARGE.builder().location(next.getKey().getLocation()).receivers(30).extra(0).spawn();
                        }
                        next.setValue(next.getValue() - 1);
                    } else {
                        reappear(next.getKey());
                        it.remove();
                    }
                } else {
                    reappear(next.getKey());
                    it.remove();
                }
            } else {
                reappear(next.getKey());
                it.remove();
            }
        }

    }

    private void reappear(Player player) {
        championsManager.getEffects().removeEffect(player, EffectType.INVISIBILITY);
        UtilServer.callEvent(new EffectExpireEvent(player, new Effect(player.getUniqueId().toString(), EffectType.INVISIBILITY, 1, 0))); // Do this incase
        UtilMessage.message(player, "Champions>", "You have reappeared.");
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (smoked.containsKey(player)) {
                if (event.getReason() != null) {
                    if (event.getReason().equalsIgnoreCase("Sever")) {
                        return;
                    }
                }

                reappear(player);
                smoked.remove(player);

            }

        }
    }


    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (smoked.containsKey(event.getPlayer())) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                smoked.remove(event.getPlayer());
                reappear(event.getPlayer());
            }
        }
    }


    @Override
    public String getName() {
        return "Smoke Bomb";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Instantly <effect>Vanish</effect> before your foes",
                "for a maximum of <val>" + (baseDuration + level) + "</val> seconds,",
                "inflicting <effect>Blindness II</effect> to enemies",
                "within <stat>" + blindRadius + "</stat> blocks for <stat>" + blindDuration + "</stat> seconds",
                "",
                "Hitting an enemy or using abilities",
                "will make you reappear",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2.5);
    }

    @Override
    public void toggle(Player player, int level) {
        player.playSound(player.getLocation(), Sound.BLOCK_CONDUIT_AMBIENT, 2.0f, 1.f);

        championsManager.getEffects().addEffect(player, EffectType.INVISIBILITY, (long) ((baseDuration + level) * 1000L));
        smoked.put(player, (int) (baseDuration + level));


        for (int i = 0; i < 3; i++) {

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 2.0F, 0.5F);
        }

        // Display particle to those only within 30 blocks
        Particle.EXPLOSION_HUGE.builder().location(player.getLocation()).receivers(30).spawn();

        for (Player target : UtilPlayer.getNearbyEnemies(player, player.getLocation(), blindRadius)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (int) (blindDuration * 20), 1));
        }

        UtilServer.callEvent(new EffectClearEvent(player));

    }

    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 3.0, Double.class);
        blindDuration = getConfig("blindDuration", 1.75, Double.class);
        blindRadius = getConfig("blindRadius", 2.5, Double.class);
    }

}
