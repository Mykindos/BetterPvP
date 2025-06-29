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

    //todo num times attacked/it


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

    //todo bloodsphere heal
    HEAL_SCYTHE("Scythe Heal", "Healing from procing the Scythe"),

    CLONE_ATTACK("Clone Attacks", "Number of times your Clone successfully attacks"),

    //clans
    SET_CORE("Set Core", "Number of times you set your clan core"),
    TELEPORT_CORE("Teleport to Core", "Number of times you teleported to your clan core"),

    //events
    //todo undead city keys collected/chests opened
    DREADBEARD_KILLS("Kill Dreadbeard", "Number of times you killed Dreadbeard"),
    SKELETON_KING_KILLS("Kill the Skeleton King", "Number of times you killed the Skeleton King");

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
            log.info("Non Composite Stat {}", name()).submit();
            return statContainer.getProperty(period,this.name());
        }

        log.info("Composite Stat {}", name()).submit();
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
