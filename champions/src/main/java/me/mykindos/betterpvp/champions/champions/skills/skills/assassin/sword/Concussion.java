package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DebuffSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

@Singleton
@BPvPListener
public class Concussion extends PrepareSkill implements CooldownSkill, Listener, DebuffSkill, OffensiveSkill {

    private double baseDuration;

    private double durationIncreasePerLevel;
    private int concussionStrength;

    @Inject
    public Concussion(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Concussion";
    }

    @Override
    public Component[] getDescription(int level) {
        Component duration = getValueComponent(this::getDuration, level);
        Component cooldown = getValueComponent(this::getCooldown, level);
        Component concuss = Translations.component("champions.skill.effect.concuss.name").color(NamedTextColor.WHITE);
        Component[] components = Translations.componentLines(
                "champions.skill.assassin.concussion.description",
                duration,
                cooldown,
                concuss
        );
        Component concussionDetail = Translations.component("champions.skill.effect.concussion",
                Component.text(UtilFormat.getRomanNumeral(concussionStrength))).color(NamedTextColor.WHITE);
        Component[] detail = Translations.componentLines(
                "champions.skill.assassin.concussion.detail",
                concussionDetail,
                Component.text(String.valueOf(concussionStrength * 25), NamedTextColor.GREEN)
        );
        Component[] result = new Component[components.length + 1 + detail.length];
        System.arraycopy(components, 0, result, 0, components.length);
        result[components.length] = Component.empty();
        System.arraycopy(detail, 0, result, components.length + 1, detail.length);
        return result;
    }

    public double getDuration(int level) {
        return baseDuration + (durationIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;
        int level = getLevel(damager);
        if (level <= 0) return;

        if (active.contains(damager.getUniqueId())) {
            event.addReason("Concussion");
            if (championsManager.getEffects().hasEffect(damagee, EffectTypes.CONCUSSED)) {
                UtilMessage.message(damager, getName(), "champions.skill.assassin.concussion.already-concussed", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damagee, damager));
                return;
            }

            championsManager.getEffects().addEffect(damagee, damager, EffectTypes.CONCUSSED, concussionStrength, (long) (getDuration(level) * 1000L));

            UtilMessage.message(damager, getName(), "champions.skill.assassin.concussion.gave", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damagee, damager));
            UtilMessage.message(damagee, getName(), "champions.skill.assassin.concussion.received", this.championsManager.getDisplayNameProvider().getDisplayNameAsComponent(damager, damagee));
            active.remove(damager.getUniqueId());
        }
    }

    @Override
    public boolean canUse(Player player) {
        if (active.contains(player.getUniqueId())) {
            UtilMessage.message(player, getClassType().getDisplayName(), "champions.skill.assassin.concussion.already-active", getDisplayName().color(NamedTextColor.GREEN));
            return false;
        }

        return true;
    }

    @Override
    public boolean activate(Player player, int level) {
        active.add(player.getUniqueId());
        return true;
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig() {
        baseDuration = getConfig("baseDuration", 1.5, Double.class);
        durationIncreasePerLevel = getConfig("durationIncreasePerLevel", 1.5, Double.class);
        concussionStrength = getConfig("concussionStrength", 1, Integer.class);
    }
}
