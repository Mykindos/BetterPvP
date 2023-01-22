package me.mykindos.betterpvp.clans.clans.insurance;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

@Data
@AllArgsConstructor
public class Insurance implements Comparable<Insurance>{

    private final long time;
    private final Material blockMaterial;
    private final String blockData;
    private final InsuranceType insuranceType;
    private final Location blockLocation;


    @Override
    public int compareTo(@NotNull Insurance o) {
        return (int) (this.time - o.time);
    }
}