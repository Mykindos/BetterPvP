package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.world.level.material.MaterialColor;

@Data
@AllArgsConstructor
public class MapPixel {

    private int colorId;
    private short averageY;

}
