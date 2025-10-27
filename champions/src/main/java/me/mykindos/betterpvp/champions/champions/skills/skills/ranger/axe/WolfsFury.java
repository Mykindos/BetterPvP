package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.axe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.BuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.StateSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class WolfsFury extends StateSkill implements Listener, OffensiveSkill, BuffSkill {
    private final WeakHashMap<Player, Integer> missedSwings = new WeakHashMap<>();

    private double baseDuration;
    private double durationIncreasePerLevel;
    private int strengthLevel;
    private int baseMissedSwings;
    private int strengthLevelIncreasePerLevel;
    private double missedSwingsIncreasePerLevel;

    @Inject
    public WolfsFury(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with an Axe to activate",
                "",
                "Summon the power of the wolf, gaining",
                "<effect>Strength " + UtilFormat.getRomanNumeral(getStrengthLevel(level)) + "</effect> for " + getValueString(this::getDuration, level) + " seconds and giving",
                "no knockback on your attacks",
                "",
                "If you miss " + getValueString(this::getMaxMissedSwings, level) + " consecutive swings",
                "Wolfs Fury ends",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "",
                EffectTypes.STRENGTH.getDescription(getStrengthLevel(level)),
        };
    }

    public double getDuration(int level) {
        return baseDuration + (level - 1) * durationIncreasePerLevel;
    }

    public double getMaxMissedSwings(int level) {
        return baseMissedSwings + ((level - 1) * missedSwingsIncreasePerLevel);
    }

    public int getStrengthLevel(int level){
        return strengthLevel + ((level - 1) * strengthLevelIncreasePerLevel);
    }

    @Override
    public void activate(Player player, int level) {
        super.activate(player, level);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 2f, 1.2f);
        championsManager.getEffects().addEffect(player, EffectTypes.STRENGTH, getName(), getStrengthLevel(level), (long) (getDuration(level) * 1000L));
    }


    @EventHandler
    public void onDamage(CustomDamageEvent e) {
        if (e.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(e.getDamager() instanceof Player damager)) return;
        if (!activeState.containsKey(damager.getUniqueId())) return;

        int level = getLevel(damager);
        if (level > 0) {
            e.setKnockback(false);
            e.addReason(getName());
        }
        missedSwings.put(damager, 0);
    }

    @Override
    protected void doOnSuccessfulUpdate(@NotNull Player player) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 0.75F);
        new ParticleBuilder(Particle.DUST)
                .location(player.getLocation().add(0, 1, 0))
                .count(1)
                .offset(0.3, 0.6, 0.3)
                .extra(0)
                .receivers(60)
                .data(dustOptions)
                .spawn();
    }

    @EventHandler (ignoreCancelled = true)
    public void onMiss(PlayerArmSwingEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        final @NotNull Player player = event.getPlayer();
        int level = getLevel(player);
        if (level <= 0) return;

        final @NotNull UUID uuid = player.getUniqueId();
        if (!activeState.containsKey(uuid)) return;

        missedSwings.put(player, missedSwings.getOrDefault(player, 0) + 1);
        if (missedSwings.get(player) >= getMaxMissedSwings(level)) {
            doWhenStateEnds(uuid);
        }
    }

    @Override
    protected void doWhenStateEnds(@NotNull UUID uuid) {
        super.doWhenStateEnds(uuid);

        final @Nullable Player player = Bukkit.getPlayer(uuid);
        missedSwings.remove(player);  // if possible, try to remove
        if (player == null) return;

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 2f, 1);
        championsManager.getEffects().removeEffect(player, EffectTypes.STRENGTH, getName());

    }

    @Override
    protected @NotNull String getActionBarLabel() {
        return "Strength";
    }

    @Override
    protected double getStateDuration(int level) {
        return getDuration(level);
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.AXE;
    }

    @Override
    public String getName() {
        return "Wolfs Fury";
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 5.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.0, Double.class);
        baseMissedSwings = getConfig("baseMissedSwings", 2, Integer.class);
        missedSwingsIncreasePerLevel = getConfig("missedSwingsIncreasePerLevel", 1.0, Double.class);
        strengthLevel = getConfig("strengthLevel", 2, Integer.class);
        strengthLevelIncreasePerLevel = getConfig("strengthLevelIncreasePerLevel", 0, Integer.class);
    }
}