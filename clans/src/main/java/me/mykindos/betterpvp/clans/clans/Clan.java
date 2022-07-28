package me.mykindos.betterpvp.clans.clans;

import lombok.Builder;
import lombok.Data;
import me.mykindos.betterpvp.clans.clans.components.ClanAlliance;
import me.mykindos.betterpvp.clans.clans.components.ClanEnemy;
import me.mykindos.betterpvp.clans.clans.components.ClanMember;
import me.mykindos.betterpvp.clans.clans.components.ClanTerritory;
import org.bukkit.Location;

import java.sql.Timestamp;
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

    private List<ClanMember> members;
    private List<ClanAlliance> alliances;
    private List<ClanEnemy> enemies;
    private List<ClanTerritory> territory;

}
