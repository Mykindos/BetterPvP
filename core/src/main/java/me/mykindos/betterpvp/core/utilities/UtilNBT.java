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
     * TODO
     * @param id
     * @return
     */
    public static Optional<CompoundTag> getPlayerData(UUID id) {
        File currentPlayerData = new File(Bukkit.getWorldContainer(), "world/playerdata/" + id + ".dat");
        //NbtCompound compound;
        CompoundTag compound;
        try {
            //compound = NbtFactory.fromFile(currentPlayerData.getPath());
            compound = NbtIo.readCompressed(Path.of(currentPlayerData.getPath()), NbtAccounter.unlimitedHeap());
            log.info(currentPlayerData.getPath()).submit();
        } catch (
                IOException exception) {
            log.error("Error getting player data {}", exception).submit();
            return Optional.empty();
        }
        return Optional.of(compound);
    }

    /**
     * TODO
     * @param id
     * @param data
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
