package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
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
public class Sever extends Skill implements CooldownSkill, Listener, OffensiveSkill, DebuffSkill {
    private double baseDuration;
    private double durationIncreasePerLevel;
    private double hitDistance;
    private WeakHashMap<Player, Boolean> rightClicked = new WeakHashMap<>();

    @Inject
    public Sever(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Sever";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component bleed = Translations.component("champions.skill.effect.bleed.name").color(NamedTextColor.WHITE);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.sever.description",
                duration,
                cooldown,
                bleed
        );
        Component bleedDetail = Translations.component("champions.skill.effect.bleed.name").color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines(
                "champions.skill.effect.bleed.detail",
                bleedDetail,
                Component.text("2.0", NamedTextColor.GREEN)
        );
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return baseDuration + ((level - 1) * durationIncreasePerLevel);
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

    private void onInteract(Player player, LivingEntity ent) {
        if (!isHolding(player)) return;

        int level = getLevel(player);
        if (level <= 0) {
            return;
        }

        final PlayerUseSkillEvent event = UtilServer.callEvent(new PlayerUseSkillEvent(player, this, level));
        if (event.isCancelled()) {
            return;
        }

        if (ent == null) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.failed", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
            return;
        }

        boolean withinRange = UtilMath.offset(player, ent) <= hitDistance;
        if (UtilPlayer.isCreativeOrSpectator(ent) || UtilEntity.getRelation(player, ent) == EntityProperty.FRIENDLY || !withinRange) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.failed", getDisplayName().color(NamedTextColor.GREEN), Component.text(String.valueOf(level), NamedTextColor.GREEN));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 0.5F);
        } else {
            championsManager.getEffects().addEffect(ent, player, EffectTypes.BLEED, 1, (long) (getDuration(level) * 1000L));
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.sever.severed", this.championsManager.getDisplayNameService().getProvider().getDisplayNameAsComponent(ent, player));
            UtilMessage.message(ent, getClassType().getDisplayName(), "champions.skill.assassin.sever.severed-by", this.championsManager.getDisplayNameService().getProvider().getDisplayNameAsComponent(player, ent));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SPIDER_HURT, 1.0F, 1.5F);
        }

    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.0, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.0, Double.class);
        hitDistance = getConfig("hitDistance", 4.0, Double.class);
    }
}
