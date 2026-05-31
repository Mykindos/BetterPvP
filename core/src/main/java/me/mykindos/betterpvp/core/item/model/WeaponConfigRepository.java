package me.mykindos.betterpvp.core.item.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;

/**
 * Syncs {@link WeaponItem} stat configuration into the {@code weapon_damage_config} database table
 * so that Grafana dashboards always reflect the current YAML config values.
 *
 * <p>Called automatically from {@link WeaponItem#reload()} on server start and on {@code /champions reload}.</p>
 */
@Singleton
@CustomLog
public class WeaponConfigRepository {

    private final Database database;

    @Inject
    public WeaponConfigRepository(Database database) {
        this.database = database;
    }

    /**
     * Upserts a single melee weapon's stat config into {@code weapon_damage_config}.
     *
     * @param weaponKey       the item key (e.g. {@code standard_sword})
     * @param weaponName      human-readable display name
     * @param weaponType      {@code "sword"}, {@code "axe"}, or {@code "special"}
     * @param damageBase      base damage stat
     * @param damageMin       per-socket minimum damage roll
     * @param damageMax       per-socket maximum damage roll
     * @param attackSpeedBase base attack-speed stat
     * @param attackSpeedMin  per-socket minimum attack-speed roll
     * @param attackSpeedMax  per-socket maximum attack-speed roll
     * @param maxSockets      maximum sockets this weapon supports
     */
    public void upsert(String weaponKey, String weaponName, String weaponType,
                       double damageBase, double damageMin, double damageMax,
                       double attackSpeedBase, double attackSpeedMin, double attackSpeedMax,
                       int maxSockets) {
        database.getAsyncDslContext().executeAsyncVoid(ctx ->
                ctx.execute("""
                        INSERT INTO weapon_damage_config
                            (weapon_key, weapon_name, weapon_type,
                             damage_base, damage_min, damage_max,
                             attack_speed_base, attack_speed_min, attack_speed_max,
                             max_sockets, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                        ON CONFLICT (weapon_key) DO UPDATE SET
                            weapon_name        = EXCLUDED.weapon_name,
                            weapon_type        = EXCLUDED.weapon_type,
                            damage_base        = EXCLUDED.damage_base,
                            damage_min         = EXCLUDED.damage_min,
                            damage_max         = EXCLUDED.damage_max,
                            attack_speed_base  = EXCLUDED.attack_speed_base,
                            attack_speed_min   = EXCLUDED.attack_speed_min,
                            attack_speed_max   = EXCLUDED.attack_speed_max,
                            max_sockets        = EXCLUDED.max_sockets,
                            updated_at         = NOW()
                        """,
                        weaponKey, weaponName, weaponType,
                        damageBase, damageMin, damageMax,
                        attackSpeedBase, attackSpeedMin, attackSpeedMax,
                        maxSockets)
        ).exceptionally(ex -> {
            log.error("Failed to upsert weapon_damage_config for {}", weaponKey, ex).submit();
            return null;
        });
    }
}

