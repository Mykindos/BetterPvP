package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Data;

@Data
public class MapSettings {

    private boolean update = false;
    private Scale scale = Scale.CLOSEST;
    private int mapX, mapZ;
    protected long lastUpdated;

    public MapSettings(int lastX, int lastZ) {
        this.lastUpdated = System.currentTimeMillis();
        this.mapX = lastX;
        this.mapZ = lastZ;
    }


    public Scale setScale(Scale scale) {
        this.scale = scale;
        return this.scale;
    }

    public enum Scale {
        CLOSEST(1),
        CLOSE(2),
        NORMAL(4),
        FAR(10),
        FARTHEST(15);

        private final int value;

        Scale(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}
