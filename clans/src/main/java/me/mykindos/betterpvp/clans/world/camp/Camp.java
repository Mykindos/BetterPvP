package me.mykindos.betterpvp.clans.world.camp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A camp as it is kept between visits: everything about it that its world does not hold by itself.
 * <p>
 * The world is built from a template and can be rebuilt from one at any time, so nothing that has to survive that
 * lives in its blocks. What does live here is what the clan chose and what it has raised, which is what makes the
 * record rather than the folder the thing worth keeping and worth moving somewhere shared later.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camp {

    /** Which template the world is built from, or null while the clan has not chosen and takes the default. */
    private @Nullable String skin;

    private List<Structure> structures = new ArrayList<>();

    /**
     * Something the clan has raised, where it stands and how far it has come along.
     * <p>
     * Nothing places one yet. The shape is here because it is what the record has to be able to hold, and adding it
     * later would mean rewriting records already written.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Structure {

        /** What was built, naming a row in whatever catalogue ends up describing them. */
        private String type;

        private int x;
        private int y;
        private int z;

        /** Which way it faces, in degrees, as the world was built rather than as anybody is standing. */
        private float yaw;

        /** How far it has been taken, which is what decides the shape it is built in. */
        private int upgrade;

        /**
         * What the containers inside it hold, by whatever name the structure gives each one.
         * <p>
         * Kept with the structure rather than with the world because a rebuild replaces every block in it, and a
         * clan's stores surviving that is the whole point of writing them down.
         */
        private Map<String, String> containers = new HashMap<>();
    }
}
