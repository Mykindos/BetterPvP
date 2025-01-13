package me.mykindos.betterpvp.champions.champions.skills.skills.warlock.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.skills.warlock.data.WreathData;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.HealthSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.display.PermanentComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Wreath extends Skill implements InteractSkill, Listener, HealthSkill, DamageSkill {

    @Getter
    private int maxCharges;
    @Getter
    private double rechargeSeconds;
    @Getter
    private double slowDuration;
    @Getter
    private double damage;
    private int slowStrength;
    @Getter
    private double healthPerEnemyHit;

    private final WeakHashMap<Player, WreathData> charges = new WeakHashMap<>();
    private final PermanentComponent actionBarComponent = new PermanentComponent(gamer -> {
        final Player player = gamer.getPlayer();

        // Only display charges in hotbar if holding the weapon
        if (player == null || !charges.containsKey(player) || !isHolding(player)) {
            return null; // Skip if not online or not charging
        }

        final int currentMaxCharges = getMaxCharges();
        final int newCharges = charges.get(player).getCharges();

        return Component.text(getName() + " ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                .append(Component.text("\u25A0".repeat(newCharges)).color(NamedTextColor.GREEN))
                .append(Component.text("\u25A0".repeat(Math.max(0, currentMaxCharges - newCharges))).color(NamedTextColor.RED));
    });

    @Inject
    public Wreath(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Wreath";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Right click with a Sword to activate",
                "",
                "Release a barrage of teeth that",
                "deal <val>" + UtilFormat.formatNumber(getDamage(), 2) + "</val> damage and apply <effect>Slowness " + UtilFormat.getRomanNumeral(slowStrength) + "</effect>",
                "to their target for <val>" + getSlowDuration() + "</val> seconds.",
                "",
                "For each enemy hit, restore <val>" + getHealthPerEnemyHit() + "</val> health.",
                "",
                "Store up to <val>" + getMaxCharges() + "</val> charges",
                "",
                "Gain a charge every: <val>" + getRechargeSeconds() + "</val> seconds"
        };
    }

    @Override
    public boolean canUse(Player player) {
        WreathData wreathData = charges.get(player);
        if (wreathData != null && wreathData.getCharges() > 0) {
            return true;
        }

        UtilMessage.simpleMessage(player, getClassType().getName(), "You don't have any <alt>" + getName() + "</alt> charges.");
        return false;
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        charges.remove(player);
        gamer.getActionBar().remove(actionBarComponent);
    }

    @Override
    public void trackPlayer(Player player, Gamer gamer) {
        charges.computeIfAbsent(player, k -> new WreathData());
        gamer.getActionBar().add(900, actionBarComponent);
    }

    private void notifyCharges(Player player, int charges) {
        UtilMessage.simpleMessage(player, getClassType().getName(), "Wreath Charges: <alt2>" + charges);
    }

    @Override
    public Role getClassType() {
        return Role.WARLOCK;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    private void processPlayerAction(Player player) {

        final Location startPos = player.getLocation().clone().subtract(player.getLocation().clone().getDirection());
        startPos.setY(Math.ceil(startPos.getY()));
        final Vector vector = startPos.clone().getDirection();
        vector.setY(0);
        final Location loc = startPos.clone().subtract(0, 1, 0).add(vector);
        final Set<LivingEntity> targets = new HashSet<>();

        final BukkitTask runnable = new BukkitRunnable() {
            @Override
            public void run() {
                loc.add(vector);

                if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {

                    loc.add(0.0D, 1.0D, 0.0D);
                    if ((!UtilBlock.airFoliage(loc.getBlock())) && UtilBlock.solid(loc.getBlock())) {
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
                final List<LivingEntity> hit = UtilEntity.getNearbyEnemies(player, fangs.getLocation(), 1.5);
                for (LivingEntity target : hit) {
                    if (targets.contains(target)) {
                        continue;
                    }

                    CustomDamageEvent dmg = new CustomDamageEvent(target, player, null, EntityDamageEvent.DamageCause.CUSTOM, getDamage(), false, getName());
                    UtilDamage.doCustomDamage(dmg);
                    championsManager.getEffects().addEffect(target, player, EffectTypes.SLOWNESS, slowStrength, (long) (getSlowDuration() * 1000));
                    UtilPlayer.health(player, getHealthPerEnemyHit());
                }
                targets.addAll(hit);

            }

        }.runTaskTimer(champions, 0, 1);
        UtilServer.runTaskLater(champions, runnable::cancel, 60);
    }

    @UpdateEvent(delay = 100)
    public void recharge() {
        final Iterator<Map.Entry<Player, WreathData>> iterator = charges.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Player, WreathData> entry = iterator.next();
            final Player player = entry.getKey();
            if (!hasSkill(player)) {
                iterator.remove();
                continue;
            }

            final WreathData data = entry.getValue();
            final int maxCharges = getMaxCharges();

            if (data.getCharges() >= maxCharges) {
                continue; // skip if already at max charges
            }

            if (!championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(), false, true, true)) {
                continue; // skip if not enough time has passed
            }

            // add a charge
            data.addCharge();
            notifyCharges(player, data.getCharges());
        }
    }


    @Override
    public void activate(Player player) {
        WreathData wreathData = charges.get(player);
        if (wreathData == null) {
            return;
        }

        final int curCharges = wreathData.getCharges();
        processPlayerAction(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 2.0f, 1.8f);

        if (curCharges >= getMaxCharges()) {
            championsManager.getCooldowns().use(player, getName(), getRechargeSeconds(), false, true, true);
        }

        final int newCharges = curCharges - 1;
        wreathData.setCharges(newCharges);
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        slowDuration = getConfig("slowDuration", 2.0, Double.class);

        damage = getConfig("damage", 2.0, Double.class);

        healthPerEnemyHit = getConfig("healthPerEnemyHit", 1.0, Double.class);

        slowStrength = getConfig("slowStrength", 2, Integer.class);

        maxCharges = getConfig("maxCharges", 3, Integer.class);

        rechargeSeconds = getConfig("rechargeSeconds", 10.0, Double.class);
    }
}
