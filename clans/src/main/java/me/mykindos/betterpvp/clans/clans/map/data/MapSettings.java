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
        CLOSEST(0),
        CLOSE(1),
        NORMAL(2),
        FAR(3),
        FARTHEST(5);

        private final byte value;

        Scale(int value) {
            this.value = (byte)value;
        }

        public byte getValue() {
            return this.value;
        }
    }
}
