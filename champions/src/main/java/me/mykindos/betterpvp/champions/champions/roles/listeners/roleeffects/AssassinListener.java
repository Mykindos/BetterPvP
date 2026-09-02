package me.mykindos.betterpvp.champions.champions.roles.listeners.roleeffects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleEffect;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.champions.combat.RoleBowService;
import me.mykindos.betterpvp.core.combat.CombatFeaturesService;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

@BPvPListener
@Singleton
public class AssassinListener implements Listener, ConfigAccessor {

    // <editor-fold defaultstate="collapsed" desc="Config">
    private boolean meleeDealsNoKnockbackIsEnabled;
    private boolean meleeDealsNoKnockbackIsBuff;

    private boolean noKnockbackReceivedWhenSlowedIsEnabled;
    private boolean noKnockbackReceivedWhenSlowedIsBuff;

    // </editor-fold>

    private final RoleManager roleManager;
    private final EffectManager effectManager;
    private final CombatFeaturesService combatFeaturesService;
    private final ChampionsSkillManager skillManager;
    private final RoleBowService roleBowService;

    @Inject
    public AssassinListener(Champions champions, RoleManager roleManager, EffectManager effectManager,
                            CombatFeaturesService combatFeaturesService, ChampionsSkillManager skillManager,
                            RoleBowService roleBowService) {
        this.roleManager = roleManager;
        this.effectManager = effectManager;
        this.combatFeaturesService = combatFeaturesService;
        this.skillManager = skillManager;
        this.roleBowService = roleBowService;
        loadConfig(champions.getConfig());

        ArrayList<RoleEffect> sinPassives = RoleManager.rolePassiveDescs.getOrDefault(Role.ASSASSIN, new ArrayList<>());
        if (meleeDealsNoKnockbackIsEnabled) {
            TextComponent meleeDealsNoKnockbackDescription = Component.text("Melee attacks deal no knockback");
            sinPassives.add(new RoleEffect(meleeDealsNoKnockbackDescription, meleeDealsNoKnockbackIsBuff));
        }

        if (noKnockbackReceivedWhenSlowedIsEnabled) {
            TextComponent noKnockbackReceivedWhenSlowedDescription = Component.text("Cannot be knocked back while ")
                    .append(Component.text("Slowed").color(NamedTextColor.WHITE));
            sinPassives.add(new RoleEffect(noKnockbackReceivedWhenSlowedDescription, noKnockbackReceivedWhenSlowedIsBuff));
        }

        // Need this line since we probably had to create a new ArrayList
        RoleManager.rolePassiveDescs.put(Role.ASSASSIN, sinPassives);
    }

    /**
     * Surgical Precision Passive & Speedlock Passive
     */
    @EventHandler
    public void onAssassinKnockback(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;

        if (meleeDealsNoKnockbackIsEnabled && event.getDamager() instanceof Player damager && isAssassin(damager)) {
            event.setKnockback(false);
        }

        if (noKnockbackReceivedWhenSlowedIsEnabled && event.getDamagee() instanceof Player damagee && isAssassin(damagee)) {
            if (effectManager.hasEffect(damagee, EffectTypes.SLOWNESS)) {
                event.setKnockback(false);
            }
        }
    }

    /**
     * With this on, an assassin's bow is purely a delivery tool for their bow skills - loosing it without
     * a prepared skill does nothing. Cancelling here at {@link EventPriority#LOWEST} keeps the preparation
     * intact, since {@code PrepareArrowSkill} only spends it on an uncancelled shot.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAssassinShootBow(EntityShootBowEvent event) {
        if (!roleBowService.isOnlyWhilePrepared(Role.ASSASSIN)) return;
        if (!(event.getEntity() instanceof Player player) || !isAssassin(player)) return;
        if (hasPreparedBowSkill(player)) return;

        event.setCancelled(true);
        player.sendActionBar(Translations.component("champions.combat.assassin-bow-not-prepared")
                .color(NamedTextColor.RED));
    }

    private boolean isAssassin(Player player) {
        return roleManager.hasRole(player, Role.ASSASSIN) && combatFeaturesService.isActive(player);
    }

    private boolean hasPreparedBowSkill(Player player) {
        return skillManager.getObjects().values().stream()
                .filter(skill -> skill.getType() == SkillType.BOW)
                .filter(PrepareSkill.class::isInstance)
                .anyMatch(skill -> ((PrepareSkill) skill).isPrepared(player));
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.meleeDealsNoKnockbackIsEnabled = config.getOrSaveBoolean("class.assassin.melee-deals-no-knockback.enabled", true);
        this.meleeDealsNoKnockbackIsBuff = config.getOrSaveBoolean("class.assassin.melee-deals-no-knockback.isBuff", true);

        this.noKnockbackReceivedWhenSlowedIsEnabled = config.getOrSaveBoolean("class.assassin.no-knockback-received-when-slowed.enabled", true);
        this.noKnockbackReceivedWhenSlowedIsBuff = config.getOrSaveBoolean("class.assassin.no-knockback-received-when-slowed.isBuff", false);
    }
}
