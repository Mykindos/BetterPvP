package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.utility.StatValueType;
import me.mykindos.betterpvp.core.server.Period;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Set;

@Getter
@CustomLog
public enum ClientStat implements IClientStat {
    PLAYER_KILLS(StatValueType.LONG, "Players Killed", "The number of players you have killed"),
    PLAYER_KILL_ASSISTS(StatValueType.LONG, "Player Assists", "The number of players that you have assisted in their deaths"),
    PLAYER_DEATHS(StatValueType.LONG, "Death from Players", "The number of times you have died to another player"),

    REGENERATION_EFFECT_TO_OTHERS(StatValueType.DOUBLE, "Healing others with regeneration", "Healing other players with the regeneration effect", "You gave them"),
    REGENERATION_EFFECT_FROM_OTHERS(StatValueType.DOUBLE, "Healing received from others by regeneration", "Healing by regeneration from other sources"),
    REGENERATION_EFFECT_SELF(StatValueType.DOUBLE, "Healing self with regeneration", "Healing yourself with the regeneration effect"),

    TIME_PLAYED(StatValueType.DURATION, "Time Played", Material.CLOCK, 0, false, "Time spent playing"),

    //champions
    HEAL_DEALT_DEFENSIVE_AURA(StatValueType.DOUBLE, "Defensive Aura Heal", "Healing done using defensive aura"),
    HEAL_RECEIVED_DEFENSIVE_AURA(StatValueType.DOUBLE, "Defensive Aura Receive Heal", "Healing received from defensive Aura"),
    HEAL_SELF_DEFENSIVE_AURA(StatValueType.DOUBLE, "Defensive Aura Self Healing", "Healing done to self by defensive Aura"),

    HEAL_RECALL(StatValueType.DOUBLE, "Recall Heal", "Healing received from using recall"),
    HEAL_BLOODLUST(StatValueType.DOUBLE, "Bloodlust Heal", "Healing received from procing Bloodlust"),
    HEAL_FORTITUDE(StatValueType.DOUBLE, "Fortitude Heal", "Healing received from procing Fortitude"),
    HEAL_RIPOSTE(StatValueType.DOUBLE, "Riposte Heal", "Healing received from procing Riposte"),

    HEAL_DEALT_BIOTIC_QUIVER(StatValueType.DOUBLE, "Biotic Quiver Heal", "Healing done using Biotic Quiver"),
    HEAL_RECEIVED_BIOTIC_QUIVER(StatValueType.DOUBLE, "Biotic Quiver Receive Heal", "Healing received using Biotic Quiver"),
    HEAL_SELF_BIOTIC_QUIVER(StatValueType.DOUBLE, "Biotic Quiver Self Heal", "Healing done to yourself with Biotic Quiver"),

    HEAL_VITALITY_SPORES(StatValueType.DOUBLE, "Vitality Spores Heal", "Healing using Vitality Spores"),
    HEAL_CLONE(StatValueType.DOUBLE, "Clone Heal", "Healing from clone procing"),
    HEAL_SIPHON(StatValueType.DOUBLE, "Siphon Heal", "Healing from procing Siphon"),
    HEAL_LEECH(StatValueType.DOUBLE, "Leech Heal", "Healing from procing Leach"),
    HEAL_WREATH(StatValueType.DOUBLE, "Wreath Heal", "Healing from procing Wreath"),

    HEAL_SCYTHE(StatValueType.DOUBLE, "Scythe Heal", "Healing from procing the Scythe"),

    CLONE_ATTACK(StatValueType.DOUBLE, "Clone Attacks", "Number of times your Clone successfully attacks"),

    //clans
    CLANS_SET_CORE(StatValueType.LONG, "Set Core", "Number of times you set your clan core"),
    CLANS_TELEPORT_CORE(StatValueType.LONG, "Teleport to Core", "Number of times you teleported to your clan core"),

    /**
     * Multiplied by 1000 when storing
     */
    CLANS_CLANS_EXPERIENCE(StatValueType.DOUBLE, "Clan Experience", "Clan experience you earned"),

    CLANS_ENERGY_DROPPED(StatValueType.LONG, "Energy Dropped", "Energy dropped as a result of your actions", "I.e. killing a boss, mining a block"),

    CLANS_ENERGY_COLLECTED(StatValueType.LONG, "Energy Collected", "Amount of Energy you picked up for your Clan"),

    CLANS_CLAIM_TERRITORY(StatValueType.LONG, "Claim Territory", "Amount of territory you claimed for your clan"),
    CLANS_UNCLAIM_TERRITORY(StatValueType.LONG, "Unclaim Territory", "Amount of territory you unclaimed for your clan"),
    CLANS_UNCLAIM_OTHER_TERRITORY(StatValueType.LONG, "Unclaim Enemy Territory", "Amount of territory you unclaimed from another Clan"),

    CLANS_ATTACK_PILLAGE(StatValueType.LONG, "Pillage (Attacker)", "Number of times you pillaged another Clan"),
    CLANS_DEFEND_PILLAGE(StatValueType.LONG, "Pillage (Defender)", "Number of times your Clan was pillaged"),

    CLANS_DESTROY_CORE(StatValueType.LONG, "Destroy Core", "Number of times you destroyed an opposing Clan's core"),
    CLANS_CORE_DESTROYED(StatValueType.LONG, "Core Destroyed", "Number of times your Clan was destroyed"),
    /**
     * Multiplied by 1000 when storing
     */
    CLANS_CORE_DAMAGE(StatValueType.DOUBLE, "Core Damage", "Amount of damage you dealt to the core"),

    CLANS_CANNON_SHOT(StatValueType.LONG, "Cannon Shots", "Number of times you fired a cannon"),
    CLANS_CANNON_BLOCK_DAMAGE(StatValueType.LONG, "Cannon Block Damage", "Amount of block damage you have dealt with cannons"),

    /**
     * Multiplied by 1000 when storing
     */
    CLANS_DOMINANCE_GAINED(StatValueType.DOUBLE, "Dominance Gained", "Dominance you gained for your Clan"),
    /**
     * Multiplied by 1000 when storing
     */
    CLANS_DOMINANCE_LOST(StatValueType.DOUBLE, "Dominance Lost", "Dominance you lost for your Clan"),

    CLANS_CLAN_LEAVE(StatValueType.LONG, "Clan Leave", "Number of times you left a Clan"),
    CLANS_CLAN_JOIN(StatValueType.LONG, "Join Clan", "Number of times you joined a Clan"),
    CLANS_CLAN_CREATE(StatValueType.LONG, "Create Clan", "Number of Clans you created"),

    HEALING_DEALT(StatValueType.DOUBLE, "Healing Dealt",
            Set.of(
                    REGENERATION_EFFECT_TO_OTHERS,
                    REGENERATION_EFFECT_SELF,
                    HEAL_DEALT_DEFENSIVE_AURA,
                    HEAL_SELF_DEFENSIVE_AURA,
                    HEAL_RECALL,
                    HEAL_BLOODLUST,
                    HEAL_FORTITUDE,
                    HEAL_RIPOSTE,
                    HEAL_DEALT_BIOTIC_QUIVER,
                    HEAL_SELF_BIOTIC_QUIVER,
                    HEAL_VITALITY_SPORES,
                    HEAL_CLONE,
                    HEAL_SIPHON,
                    HEAL_LEECH,
                    HEAL_WREATH,
                    HEAL_SCYTHE
                    ),
            "All Healing you have ever done"),

    //events
    //todo undead city keys collected

    //EVENT_UNDEAD_CITY_SPAWN_KEY("Spawn Keys", "Cause keys to drop"),
    EVENT_UNDEAD_CITY_OPEN_CHEST(StatValueType.LONG, "Open Chest", "Amount of chests you opened");

    //dungeons
    //todo dungeon mob kills

    private final StatValueType statValueType;
    private final String name;
    private final String[] description;
    @Nullable
    private final CompositeStat compositeStat;
    private final Material material;
    private final int customModelData;
    private final boolean glowing;


    ClientStat(StatValueType type, String name, String... description) {
        this(type, name, null, description);
    }

    ClientStat(StatValueType type, String name, Material material, int customModelData, boolean glowing, String... description) {
        this(type, name, material, customModelData, glowing, null, description);
    }

    ClientStat(StatValueType type, String name, Set<IStat> compositeStats, String... description) {
        this(type, name, Material.BOOK, 0, false, compositeStats, description);
    }

    ClientStat(StatValueType type, String name, Material material, int customModelData, boolean glowing, Set<IStat> compositeStats, String... description) {
        this.statValueType = type;
        this.name = name;
        this.description = description;
        this.compositeStat = compositeStats != null ? new CompositeStat(getStatType(), compositeStats) : null;
        this.material = material;
        this.customModelData = customModelData;
        this.glowing = glowing;
    }



    @Override
    public Long getStat(StatContainer statContainer, StatFilterType type, @Nullable Period period) {
        if (compositeStat == null) {
            return statContainer.getProperty(type, period,this);
        }
        return compositeStat.getStat(statContainer, type, period);
    }

    @Override
    public @NotNull String getStatType() {
        return name();
    }

    /**
     * Get the jsonb data in string format for this object
     *
     * @return
     */
    @Override
    public @Nullable JSONObject getJsonData() {
        return null;
    }

    /**
     * Get the simple name of this stat, without qualifications (if present)
     * <p>
     * i.e. Time Played, Flags Captured
     *
     * @return the simple name
     */
    @Override
    public String getSimpleName() {
        return getName();
    }

    /**
     * Get the qualified name of the stat, if one exists.
     * Should usually end with the {@link IStat#getSimpleName()}
     * <p>
     * i.e. Domination Time Played, Capture the Flag CTF_Oakvale Flags Captured
     *
     * @return the qualified name
     */
    @Override
    public String getQualifiedName() {
        return getSimpleName();
    }

    /**
     * Whether or not this stat is directly savable to the database
     *
     * @return {@code true} if it is, {@code false} otherwise
     */
    @Override
    public boolean isSavable() {
        return compositeStat == null;
    }

    @Override
    public boolean containsStat(IStat otherStat) {
        if (compositeStat == null) {
            return this.equals(otherStat);
        }
        return compositeStat.containsStat(otherStat);
    }

    /**
     * <p>Get the generic stat that includes this stat.</p>
     * <p>{@link IStat#containsStat(IStat)} of the generic should be {@code true} for this stat</p>
     *
     * @return the generic stat
     */
    @Override
    public @NotNull IStat getGenericStat() {
        //todo add a way to get set composite parents like i.e. HEALING_DEALT for REGENERATION_EFFECT_TO_OTHERS ??
        return this;
    }
}
