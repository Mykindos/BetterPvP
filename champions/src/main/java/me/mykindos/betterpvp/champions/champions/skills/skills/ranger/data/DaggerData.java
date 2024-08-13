package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Getter
@Setter
public class DaggerData {
    private Player player;
    private ItemDisplay swordDisplay;
    private Location startLocation;
    private Vector direction;
    private Location hitLocation;
    private long throwTime;
    private boolean grounded;

    public DaggerData(Player player, ItemDisplay swordDisplay, Location startLocation, Vector direction, Location hitLocation, long throwTime, boolean grounded) {
        this.player = player;
        this.swordDisplay = swordDisplay;
        this.startLocation = startLocation;
        this.direction = direction;
        this.hitLocation = hitLocation;
        this.throwTime = throwTime;
        this.grounded = grounded;
    }
}