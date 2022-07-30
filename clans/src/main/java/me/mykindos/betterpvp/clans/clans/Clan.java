package me.mykindos.betterpvp.clans.clans;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import me.mykindos.betterpvp.clans.clans.components.ClanAlliance;
import me.mykindos.betterpvp.clans.clans.components.ClanEnemy;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.components.ClanTerritory;
import org.bukkit.Location;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class Clan {

    private int id;
    private String name;
    private Timestamp timeCreated;
    private Timestamp lastLogin;
    private Location home;
    private int energy;
    private int points;
    private int level;
    private long cooldown;

    private boolean admin;
    private boolean safe;

    @Builder.Default
    private List<ClanMember> members = new ArrayList<>();

    @Builder.Default
    private List<ClanAlliance> alliances = new ArrayList<>();

    @Builder.Default
    private List<ClanEnemy> enemies = new ArrayList<>();

    @Builder.Default
    private List<ClanTerritory> territory = new ArrayList<>();

}
