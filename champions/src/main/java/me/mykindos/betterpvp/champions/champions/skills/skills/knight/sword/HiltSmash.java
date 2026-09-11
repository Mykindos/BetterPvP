package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class HiltSmash extends Skill implements CooldownSkill, Listener, OffensiveSkill, DamageSkill, DebuffSkill {

    private final WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();
    private double baseDamage;
    private double damageIncreasePerLevel;
    private double baseDuration;
    private double durationIncreasePerLevel;
    private int slowStrength;
    private double hitDistance;

    @Inject
    public HiltSmash(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Hilt Smash";
    }

    @Override
    public Component[] getDescription(int level) {
        Component damage = getValueComponent(this::getDamage, level);
        Component slowness = Translations.component("champions.skill.effect.slowness",
                Component.text(UtilFormat.getRomanNumeral(slowStrength))).color(NamedTextColor.WHITE);
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component[] components = Translations.componentLines(
                "champions.skill.knight.hilt-smash.description",
                damage,
                slowness,
                duration,
                cooldown
        );
        Component slownessDetail = Translations.component("champions.skill.effect.slowness.name").color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines(
                "champions.skill.knight.hilt-smash.detail",
                slownessDetail,
                Component.text(String.valueOf(15 * slowStrength), NamedTextColor.GREEN)
        );
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDamage(int level) {
        return baseDamage + ((level - 1) * damageIncreasePerLevel);
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;
        if (event.getRightClicked() instanceof LivingEntity entity) {
            rightClicked.put(event.getPlayer(), true);
            onInteract(event.getPlayer(), entity);
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND || !event.getAction().isRightClick()) return;
        if (UtilBlock.usable(event.getClickedBlock())) return;
        if (!rightClicked.getOrDefault(event.getPlayer(), false)) { // This means onInteract wasn't called through onEntityInteract
            if (championsManager.getCooldowns().hasCooldown(event.getPlayer(), "DoorAccess")) return;
            onInteract(event.getPlayer(), null);
        }
        rightClicked.remove(event.getPlayer()); // Reset the flag for next interactions
    }

    @Override
    public void invalidatePlayer(Player player, Gamer gamer) {
        rightClicked.remove(player);
    }

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level <= 0) {
            return;
        }

        final PlayerUseSkillEvent skillEvent = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
        if (skillEvent.isCancelled()) {
            return;
        }

        boolean withinRange = ent != null && UtilMath.offset(player, ent) <= hitDistance;
        boolean isFriendly = false;

        if (ent instanceof Player damagee) {
            isFriendly = UtilEntity.getRelation(player, damagee) == EntityProperty.FRIENDLY;
        }

        if (ent == null || !withinRange || isFriendly) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.knight.hilt-smash.failed", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0F, 0.0F);
        } else {
            UtilMessage.message(ent, getClassType().getDisplayName(), "champions.skill.hit-by", this.championsManager.getDisplayNameAsComponent(player, ent), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
            championsManager.getEffects().addEffect(ent, player, EffectTypes.SLOWNESS, slowStrength, (long) (getDuration(level) * 1000));
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.hit-target", this.championsManager.getDisplayNameAsComponent(ent, player), getDisplayName().color(NamedTextColor.GREEN).append(Component.text(" " + level, NamedTextColor.GREEN)));
            UtilDamage.doDamage(new DamageEvent(ent, player, null, new SkillDamageCause(this), getDamage(level), getName()));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0F, 1.2F);
        }
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level-1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 3.0, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
        baseDuration = getConfig("baseDuration", 0.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 0.5, Double.class);
        slowStrength = getConfig("slowStrength", 3, Integer.class);
        hitDistance = getConfig("hitDistance", 4.0, Double.class);
    }
}
