package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.ActiveToggleSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ChargeSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergyChannelSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableRegistry;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.model.ArmorItem;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.jooq.DSLContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Syncs all Champions game configuration into dedicated Grafana tables so that
 * dashboards can visualise balance data (TTK, DPS, energy usage, etc.) without
 * polling live game state.
 *
 * <p>Called once from {@link me.mykindos.betterpvp.champions.Champions#onEnable()}
 * after items and skills have been loaded. Re-running on reload keeps the DB
 * in sync with the YAML files.</p>
 *
 * <p>Tables populated:</p>
 * <ul>
 *   <li>{@code grafana_weapon_config}    – melee-weapon damage &amp; attack-speed stats</li>
 *   <li>{@code grafana_armor_config}     – armor health &amp; durability per piece</li>
 *   <li>{@code grafana_role_config}      – role base health</li>
 *   <li>{@code grafana_skill_config}     – skill cooldown / energy / max-level config</li>
 *   <li>{@code grafana_rune_config}      – per-rune config params (key-value)</li>
 *   <li>{@code grafana_energy_config}    – global energy system settings</li>
 * </ul>
 */
@Singleton
@CustomLog
public class GrafanaConfigSyncService {

    private final Database database;
    private final ItemRegistry itemRegistry;
    private final ChampionsSkillManager skillManager;
    private final SocketableRegistry socketableRegistry;
    private final EnergyService energyService;

    @Inject
    public GrafanaConfigSyncService(Database database,
                                    ItemRegistry itemRegistry,
                                    ChampionsSkillManager skillManager,
                                    SocketableRegistry socketableRegistry,
                                    EnergyService energyService) {
        this.database = database;
        this.itemRegistry = itemRegistry;
        this.skillManager = skillManager;
        this.socketableRegistry = socketableRegistry;
        this.energyService = energyService;
    }

    /**
     * Runs all config sync domains asynchronously.
     */
    public void syncAll() {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            syncWeaponConfig(ctx);
            syncArmorConfig(ctx);
            syncRoleConfig(ctx);
            syncSkillConfig(ctx);
            syncRuneConfig(ctx);
            syncEnergyConfig(ctx);
            log.info("Grafana config sync complete.").submit();
        }).exceptionally(ex -> {
            log.error("Failed to sync Grafana config tables", ex).submit();
            return null;
        });
    }

    // -------------------------------------------------------------------------
    // Weapon config
    // -------------------------------------------------------------------------

    private void syncWeaponConfig(DSLContext ctx) {
        for (Map.Entry<org.bukkit.NamespacedKey, me.mykindos.betterpvp.core.item.BaseItem> entry
                : itemRegistry.getItems().entrySet()) {

            if (!(entry.getValue() instanceof WeaponItem weapon)) continue;
            if (!weapon.getGroups().contains(WeaponItem.Group.MELEE)) continue;

            String weaponKey  = weapon.getWeaponKey();
            String weaponName = weapon.getPlainName();
            String weaponType = weapon.getWeaponType();

            double damageBase = 0, damageMin = 0, damageMax = 0;
            double speedBase  = 0, speedMin  = -0.25, speedMax = 0.25;

            var statContainerOpt = weapon.getComponent(StatContainerComponent.class);
            if (statContainerOpt.isPresent()) {
                var sc = statContainerOpt.get();

                var damageStat = sc.getStat(StatTypes.MELEE_DAMAGE);
                if (damageStat.isPresent()) {
                    var ds = damageStat.get();
                    damageBase = ds.getValue();
                    damageMin  = ds.getBaseRangeMin();
                    damageMax  = ds.getRangeMax();
                }

                var speedStat = sc.getStat(StatTypes.MELEE_ATTACK_SPEED);
                if (speedStat.isPresent()) {
                    var ss = speedStat.get();
                    speedBase = ss.getValue();
                    speedMin  = ss.getBaseRangeMin();
                    speedMax  = ss.getRangeMax();
                }
            }

            // max_sockets: 4 is the practical ceiling for rune slots across all purities
            final int maxSockets = 4;

            ctx.execute("""
                    INSERT INTO grafana_weapon_config
                        (weapon_key, weapon_name, weapon_type,
                         damage_base, damage_min, damage_max,
                         attack_speed_base, attack_speed_min, attack_speed_max,
                         max_sockets, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (weapon_key) DO UPDATE SET
                        weapon_name       = EXCLUDED.weapon_name,
                        weapon_type       = EXCLUDED.weapon_type,
                        damage_base       = EXCLUDED.damage_base,
                        damage_min        = EXCLUDED.damage_min,
                        damage_max        = EXCLUDED.damage_max,
                        attack_speed_base = EXCLUDED.attack_speed_base,
                        attack_speed_min  = EXCLUDED.attack_speed_min,
                        attack_speed_max  = EXCLUDED.attack_speed_max,
                        max_sockets       = EXCLUDED.max_sockets,
                        updated_at        = NOW()
                    """,
                    weaponKey, weaponName, weaponType,
                    damageBase, damageMin, damageMax,
                    speedBase, speedMin, speedMax,
                    maxSockets);
        }
    }

    // -------------------------------------------------------------------------
    // Armor config
    // -------------------------------------------------------------------------

    private void syncArmorConfig(DSLContext ctx) {
        for (Map.Entry<org.bukkit.NamespacedKey, me.mykindos.betterpvp.core.item.BaseItem> entry
                : itemRegistry.getItems().entrySet()) {

            if (!(entry.getValue() instanceof ArmorItem armor)) continue;

            String itemKey  = entry.getKey().getKey();
            String itemName = armor.getPlainName();
            Material material = armor.getModel().getType();

            // Determine equipment slot from material
            String slot = resolveSlot(material);

            // Match to a role using the Role enum's material fields
            String roleName = null;
            for (Role role : Role.values()) {
                if (material == role.getHelmet()
                        || material == role.getChestplate()
                        || material == role.getLeggings()
                        || material == role.getBoots()) {
                    roleName = role.getName();
                    break;
                }
            }

            double healthBase = 1.0, healthMin = 0.0, healthMax = 2.0;
            int    durability = 500;

            var scOpt = armor.getComponent(StatContainerComponent.class);
            if (scOpt.isPresent()) {
                var healthStatOpt = scOpt.get().getStat(StatTypes.HEALTH);
                if (healthStatOpt.isPresent()) {
                    var hs = healthStatOpt.get();
                    healthBase = hs.getValue();
                    healthMin  = hs.getBaseRangeMin();
                    healthMax  = hs.getRangeMax();
                }
            }

            var durOpt = armor.getComponent(DurabilityComponent.class);
            if (durOpt.isPresent()) {
                durability = durOpt.get().getMaxDamage();
            }

            ctx.execute("""
                    INSERT INTO grafana_armor_config
                        (item_key, item_name, role_name, slot,
                         health_base, health_min, health_max, durability, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (item_key) DO UPDATE SET
                        item_name   = EXCLUDED.item_name,
                        role_name   = EXCLUDED.role_name,
                        slot        = EXCLUDED.slot,
                        health_base = EXCLUDED.health_base,
                        health_min  = EXCLUDED.health_min,
                        health_max  = EXCLUDED.health_max,
                        durability  = EXCLUDED.durability,
                        updated_at  = NOW()
                    """,
                    itemKey, itemName, roleName, slot,
                    healthBase, healthMin, healthMax, durability);
        }
    }

    /** Maps a Bukkit {@link Material} to a human-readable armor slot name, or {@code null}. */
    private static String resolveSlot(Material material) {
        try {
            EquipmentSlot slot = material.getEquipmentSlot();
            return switch (slot) {
                case HEAD -> "HELMET";
                case CHEST -> "CHESTPLATE";
                case LEGS -> "LEGGINGS";
                case FEET -> "BOOTS";
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Role config
    // -------------------------------------------------------------------------

    private void syncRoleConfig(DSLContext ctx) {
        for (Role role : Role.values()) {
            ctx.execute("""
                    INSERT INTO grafana_role_config (role_name, base_health, updated_at)
                    VALUES (?, ?, NOW())
                    ON CONFLICT (role_name) DO UPDATE SET
                        base_health = EXCLUDED.base_health,
                        updated_at  = NOW()
                    """,
                    role.getName(), role.getHealth());
        }
    }

    // -------------------------------------------------------------------------
    // Skill config
    // -------------------------------------------------------------------------

    private void syncSkillConfig(DSLContext ctx) {
        for (Skill skill : skillManager.getObjects().values()) {
            String roleName  = skill.getClassType() != null ? skill.getClassType().getName() : null;
            String skillType = skill.getType().name();

            Double cooldown                    = null;
            Double cooldownDecPerLevel         = null;
            Integer energy                     = null;
            Double energyDecPerLevel           = null;
            Double energyStartCost             = null;
            Double energyStartCostDecPerLevel  = null;
            Double baseCharge                  = null;
            Double chargeIncPerLevel           = null;

            if (skill instanceof CooldownSkill) {
                cooldown            = skill.getCooldown();
                cooldownDecPerLevel = skill.getCooldownDecreasePerLevel();
            }

            if (skill instanceof EnergySkill || skill instanceof EnergyChannelSkill) {
                energy            = skill.getEnergy();
                energyDecPerLevel = skill.getEnergyDecreasePerLevel();
            }

            if (skill instanceof ActiveToggleSkill) {
                energyStartCost            = skill.getEnergyStartCost();
                energyStartCostDecPerLevel = skill.getEnergyStartCostDecreasePerLevel();
            }

            if (skill instanceof ChargeSkill) {
                baseCharge       = skill.getBaseCharge();
                chargeIncPerLevel = skill.getChargeIncreasePerLevel();
            }

            ctx.execute("""
                    INSERT INTO grafana_skill_config
                        (skill_name, role_name, skill_type, enabled, max_level,
                         cooldown, cooldown_decrease_per_level,
                         energy, energy_decrease_per_level,
                         energy_start_cost, energy_start_cost_decrease_per_level,
                         base_charge, charge_increase_per_level,
                         updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                    ON CONFLICT (skill_name) DO UPDATE SET
                        role_name                            = EXCLUDED.role_name,
                        skill_type                           = EXCLUDED.skill_type,
                        enabled                              = EXCLUDED.enabled,
                        max_level                            = EXCLUDED.max_level,
                        cooldown                             = EXCLUDED.cooldown,
                        cooldown_decrease_per_level          = EXCLUDED.cooldown_decrease_per_level,
                        energy                               = EXCLUDED.energy,
                        energy_decrease_per_level            = EXCLUDED.energy_decrease_per_level,
                        energy_start_cost                    = EXCLUDED.energy_start_cost,
                        energy_start_cost_decrease_per_level = EXCLUDED.energy_start_cost_decrease_per_level,
                        base_charge                          = EXCLUDED.base_charge,
                        charge_increase_per_level            = EXCLUDED.charge_increase_per_level,
                        updated_at                           = NOW()
                    """,
                    skill.getName(), roleName, skillType,
                    skill.isEnabled(), skill.getMaxLevel(),
                    cooldown, cooldownDecPerLevel,
                    energy, energyDecPerLevel,
                    energyStartCost, energyStartCostDecPerLevel,
                    baseCharge, chargeIncPerLevel);
        }
    }

    // -------------------------------------------------------------------------
    // Rune config
    // -------------------------------------------------------------------------

    /**
     * Uses reflection to extract numeric / boolean config fields from each rune's
     * concrete class (non-static, non-Provider declared fields only).
     */
    private void syncRuneConfig(DSLContext ctx) {
        for (Socketable rune : socketableRegistry.getAllRunes()) {
            String runeKey  = rune.getKey().getKey();
            String runeName = rune.getName();

            Class<?> runeClass = rune.getClass();
            for (Field field : runeClass.getDeclaredFields()) {
                // Skip static fields (e.g. KEY constant)
                if (Modifier.isStatic(field.getModifiers())) continue;
                // Skip Guice Provider fields (itemProvider)
                if (Provider.class.isAssignableFrom(field.getType())) continue;

                field.setAccessible(true);
                try {
                    Object value = field.get(rune);
                    if (value == null) continue;

                    // Only store primitives / numbers / booleans — skip complex objects
                    if (!(value instanceof Number) && !(value instanceof Boolean)) continue;

                    ctx.execute("""
                            INSERT INTO grafana_rune_config
                                (rune_key, rune_name, param_key, param_value, updated_at)
                            VALUES (?, ?, ?, ?, NOW())
                            ON CONFLICT (rune_key, param_key) DO UPDATE SET
                                rune_name   = EXCLUDED.rune_name,
                                param_value = EXCLUDED.param_value,
                                updated_at  = NOW()
                            """,
                            runeKey, runeName, field.getName(), String.valueOf(value));

                } catch (IllegalAccessException e) {
                    log.warn("Could not read field '{}' from rune '{}'", field.getName(), runeKey).submit();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Energy system config
    // -------------------------------------------------------------------------

    private void syncEnergyConfig(DSLContext ctx) {
        upsertEnergyParam(ctx, "maxEnergy",             energyService.getMaxEnergy());
        upsertEnergyParam(ctx, "energyPerSecond",       energyService.getEnergyPerSecond());
        upsertEnergyParam(ctx, "nerfedEnergyPerSecond", energyService.getNerfedEnergyPerSecond());
    }

    private void upsertEnergyParam(DSLContext ctx, String key, double value) {
        ctx.execute("""
                INSERT INTO grafana_energy_config (param_key, param_value, updated_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (param_key) DO UPDATE SET
                    param_value = EXCLUDED.param_value,
                    updated_at  = NOW()
                """,
                key, value);
    }
}



