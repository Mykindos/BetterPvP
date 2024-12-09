package me.mykindos.betterpvp.core.utilities;

import lombok.CustomLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@CustomLog
public class UtilNBT {

    /**
     * Gets the player data from the playerdata.dat file
     * @param id the id of the player
     * @return Optional of the playerdata as a CompoundTag
     */
    public static Optional<CompoundTag> getPlayerData(UUID id) {
        File currentPlayerData = new File(Bukkit.getWorldContainer(), "world/playerdata/" + id + ".dat");
        CompoundTag compound;
        try {
            compound = NbtIo.readCompressed(Path.of(currentPlayerData.getPath()), NbtAccounter.unlimitedHeap());
        } catch (IOException exception) {
            log.error("Error getting player data {}", exception).submit();
            return Optional.empty();
        }
        return Optional.of(compound);
    }

    /**
     * Saves the playerdata (in the form of a CompoundTag)
     * @param id the UUID of the player
     * @param data the full playerdata to save
     */
    public static void savePlayerData(UUID id, CompoundTag data) {
        File currentPlayerData = new File(Bukkit.getWorldContainer(), "world/playerdata/" + id + ".dat");
        try {
            NbtIo.writeCompressed(data, Path.of(currentPlayerData.getPath()));
            log.info(currentPlayerData.getPath()).submit();
        } catch (IOException exception) {
            log.error("Error saving player data {}", exception).submit();
        }
    }


}
