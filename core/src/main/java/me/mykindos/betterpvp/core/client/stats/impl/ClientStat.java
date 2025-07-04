package me.mykindos.betterpvp.core.client.stats.impl;

import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Getter
@CustomLog
public enum ClientStat implements IClientStat {
    REGENERATION_EFFECT_TO_OTHERS("Healing others with regeneration", "Healing other players with the regeneration effect", "You gave them"),
    REGENERATION_EFFECT_FROM_OTHERS("Healing received from others by regeneration", "Healing by regeneration from other sources"),
    REGENERATION_EFFECT_SELF("Healing self with regeneration", "Healing yourself with the regeneration effect"),

    HEALING_DEALT("Healing Dealt",
            Set.of(
                    REGENERATION_EFFECT_TO_OTHERS,
                    REGENERATION_EFFECT_SELF
            ),
            "All Healing you have ever done"),

    TIME_PLAYED("Time Played", Material.CLOCK, 0, false, "Time spent playing"),

    //champions
    HEAL_DEALT_DEFENSIVE_AURA("Defensive Aura Heal", "Healing done using defensive aura"),
    HEAL_RECEIVED_DEFENSIVE_AURA("Defensive Aura Receive Heal", "Healing received from defensive Aura"),
    HEAL_SELF_DEFENSIVE_AURA("Defensive Aura Self Healing", "Healing done to self by defensive Aura"),

    HEAL_RECALL("Recall Heal", "Healing received from using recall"),
    HEAL_BLOODLUST("Bloodlust Heal", "Healing received from procing Bloodlust"),
    HEAL_FORTITUDE("Fortitude Heal", "Healing received from procing Fortitude"),
    HEAL_RIPOSTE("Riposte Heal", "Healing received from procing Riposte"),

    HEAL_DEALT_BIOTIC_QUIVER("Biotic Quiver Heal", "Healing done using Biotic Quiver"),
    HEAL_RECEIVED_BIOTIC_QUIVER("Biotic Quiver Receive Heal", "Healing received using Biotic Quiver"),
    HEAL_SELF_BIOTIC_QUIVER("Biotic Quiver Self Heal", "Healing done to yourself with Biotic Quiver"),

    HEAL_VITALITY_SPORES("Vitality Spores Heal", "Healing using Vitality Spores"),
    HEAL_CLONE("Clone Heal", "Healing from clone procing"),
    HEAL_SIPHON("Siphon Heal", "Healing from procing Siphon"),
    HEAL_LEECH("Leech Heal", "Healing from procing Leach"),
    HEAL_WREATH("Wreath Heal", "Healing from procing Wreath"),

    HEAL_SCYTHE("Scythe Heal", "Healing from procing the Scythe"),

    CLONE_ATTACK("Clone Attacks", "Number of times your Clone successfully attacks"),

    //clans
    CLANS_SET_CORE("Set Core", "Number of times you set your clan core"),
    CLANS_TELEPORT_CORE("Teleport to Core", "Number of times you teleported to your clan core"),

    CLANS_CLANS_EXPERIENCE("Clan Experience", "Clan experience you earned"),

    CLANS_ENERGY_DROPPED("Energy Dropped", "Energy dropped as a result of your actions", "I.e. killing a boss, mining a block"),

    CLANS_ENERGY_COLLECTED("Energy Collected", "Amount of Energy you picked up for your Clan"),

    CLANS_CLAIM_TERRITORY("Claim Territory", "Amount of territory you claimed for your clan"),
    CLANS_UNCLAIM_TERRITORY("Unclaim Territory", "Amount of territory you unclaimed for your clan"),
    CLANS_UNCLAIM_OTHER_TERRITORY("Unclaim Enemy Territory", "Amount of territory you unclaimed from another Clan"),

    CLANS_ATTACK_PILLAGE("Pillage (Attacker)", "Number of times you pillaged another Clan"),
    CLANS_DEFEND_PILLAGE("Pillage (Defender)", "Number of times your Clan was pillaged"),

    CLANS_DESTROY_CORE("Destroy Core", "Number of times you destroyed an opposing Clan's core"),
    CLANS_CORE_DESTROYED("Core Destroyed", "Number of times your Clan was destroyed"),
    CLANS_CORE_DAMAGE("Core Damage", "Amount of damage you dealt to the core"),


    CLANS_CANNON_SHOT("Cannon Shots", "Number of times you fired a cannon"),
    CLANS_CANNON_BLOCK_DAMAGE("Cannon Block Damage", "Amount of block damage you have dealt with cannons"),

    CLANS_DOMINANCE_GAINED("Dominance Gained", "Dominance you gained for your Clan"),
    CLANS_DOMINANCE_LOST("Dominance Lost", "Dominance you lost for your Clan"),

    CLANS_CLAN_LEAVE("Clan Leave", "Number of times you left a Clan"),
    CLANS_CLAN_JOIN("Join Clan", "Number of times you joined a Clan"),
    CLANS_CLAN_CREATE("Create Clan", "Number of Clans you created"),

    //events
    //todo undead city keys collected/chests opened

    //EVENT_UNDEAD_CITY_SPAWN_KEY("Spawn Keys", "Cause keys to drop"),
    EVENT_UNDEAD_CITY_OPEN_CHEST("Open Chest", "Amount of chests you opened");

    //dungeons
    //todo dungeons entered, dungeons won, dungeons lost, dungeon deaths, dungeon mob kills
    private final String name;
    private final String[] description;
    @Nullable
    private final CompositeStat compositeStat;
    private final Material material;
    private final int customModelData;
    private final boolean glowing;


    ClientStat(String name, String... description) {
        this(name, null, description);
    }

    ClientStat(String name, Material material, int customModelData, boolean glowing, String... description) {
        this(name, material, customModelData, glowing, null, description);
    }

    ClientStat(String name, Set<IStat> compositeStats, String... description) {
        this(name, Material.BOOK, 0, false, compositeStats, description);
    }

    ClientStat(String name, Material material, int customModelData, boolean glowing, Set<IStat> compositeStats, String... description) {
        this.name = name;
        this.description = description;
        this.compositeStat =  compositeStats != null ? new CompositeStat(getStatName(), compositeStats) : null;
        this.material = material;
        this.customModelData = customModelData;
        this.glowing = glowing;
    }



    @Override
    public Double getStat(StatContainer statContainer, String period) {
        if (compositeStat == null) {
            return statContainer.getProperty(period,this.name());
        }
        return compositeStat.getStat(statContainer, period);
    }

    @Override
    public String getStatName() {
        return name();
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
    public boolean containsStat(String statName) {
        if (compositeStat == null) {
            return getStatName().equals(statName);
        }
        return compositeStat.containsStat(statName);
    }
}
