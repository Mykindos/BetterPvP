package me.mykindos.betterpvp.clans.clans.insurance;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Location;
import org.bukkit.Material;

@Data
@AllArgsConstructor
public class Insurance {

    private final long time;
    private final Material blockMaterial;
    private final String blockData;
    private final InsuranceType insuranceType;
    private final Location blockLocation;


}