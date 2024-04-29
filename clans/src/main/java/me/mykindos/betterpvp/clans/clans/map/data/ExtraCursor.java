package me.mykindos.betterpvp.clans.clans.map.data;

import lombok.Data;
import org.bukkit.map.MapCursor;

@Data
public class ExtraCursor {
    private int x;
    private int z;
    private boolean visible;
    private MapCursor.Type type;
    private byte direction;
    private String world;
    private boolean outside;

    /**
     * Construct a new ExtraCursor object.
     *
     * @param x         - the absolute x coordinates of the cursor
     * @param z         - the absolute z coordinates of the cursor
     * @param visible   - whether the cursor is visible or not
     * @param type      - the type/shape of the cursor
     * @param direction - the direction the cursor is pointing at
     * @param world     - the world of the cursor
     * @param outside   - whether the cursor is shown at the edge of the map, if it isn't within the bounds of the map
     */
    public ExtraCursor(int x, int z, boolean visible, MapCursor.Type type, byte direction, String world, boolean outside) {
       this.x = x;
       this.z = z;
       this.visible = visible;
       this.type = type;
       this.direction = direction;
       this.world = world;
       this.outside = outside;
    }

    /**
     * Set the direction in which the cursor is looking.
     * Values values are between 0 and 15. 0 is pointing north, between 2 values
     * are 22,5 degree rotation.
     *
     * @param direction s byte value. It makes sure that the direction value is always
     *                  between 0 and 15 by calculating the input with mod 16.
     */
    public void setDirection(byte direction) {
        this.direction = (byte) (direction % 16);
    }

    @Override
    public String toString() {
        return x + " " + z + " " + world + " " + visible;
    }

    public boolean isShownOutside() {
        return outside;
    }

    public void setShowOutside(boolean bool) {
        outside = bool;
    }
}