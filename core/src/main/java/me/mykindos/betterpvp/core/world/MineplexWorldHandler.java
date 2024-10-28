package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mineplex.studio.sdk.modules.MineplexModuleManager;
import com.mineplex.studio.sdk.modules.game.BuiltInGameState;
import com.mineplex.studio.sdk.modules.game.MineplexGame;
import com.mineplex.studio.sdk.modules.game.MineplexGameModule;
import com.mineplex.studio.sdk.modules.game.event.PostMineplexGameStateChangeEvent;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Singleton
@BPvPListener
@PluginAdapter("StudioEngine")
@CustomLog
public class MineplexWorldHandler implements Listener {

    private final Core core;

    @Inject
    public MineplexWorldHandler(Core core) {
        this.core = core;

        String rootDir = new File(".").getAbsolutePath();
        // Check if world folder exists

        try {
            // Loop through all zips in the assets/worlds folder
            for (File file : new File(rootDir + "/assets/worlds").listFiles()) {
                log.info("World folder not found for {}. Creating...", file.getName().replace(".zip", "")).submit();
                if (file.getName().endsWith(".zip") && UtilWorld.getUnloadedWorlds().stream().noneMatch(f -> f.getName().equalsIgnoreCase(file.getName().replace(".zip", "")))) {
                    unzip(file.getAbsolutePath(), Bukkit.getWorldContainer().getAbsolutePath());
                }
            }

            // Loop through all files in Bukkit.getWorldcontainer
            for (File file : new File(Bukkit.getWorldContainer().getAbsolutePath()).listFiles()) {
                log.info(file.getName()).submit();
            }
        } catch (IOException e) {
            log.error("Failed to unzip world.zip", e).submit();
        }

        UtilServer.runTaskLater(core, this::registerGame, 1L);
    }

    private void unzip(String zipFilePath, String destDir) throws IOException {
        File dir = new File(destDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        FileInputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry = zis.getNextEntry();

        while (entry != null) {
            String filePath = destDir + File.separator + entry.getName();
            Path destPath = Paths.get(destDir).resolve(entry.getName()).normalize();

            if (!destPath.startsWith(Paths.get(destDir).normalize())) {
                throw new IOException("Entry is outside of the target dir: " + entry.getName());
            }

            if (!entry.isDirectory()) {
                extractFile(zis, destPath.toString());
            } else {
                File dirEntry = new File(filePath);
                dirEntry.mkdirs();
            }
            zis.closeEntry();
            entry = zis.getNextEntry();
        }

        zis.close();
        fis.close();
    }

    private void extractFile(ZipInputStream zis, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read;
        while ((read = zis.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private void registerGame() {
        final MineplexGameModule gameModule = MineplexModuleManager.getRegisteredModule(
                MineplexGameModule.class);

        MineplexGame game = new BetterPVPMineplexGame();
        gameModule.setCurrentGame(game);
        game.setGameState(BuiltInGameState.STARTED);

        Bukkit.getPluginManager().callEvent(new PostMineplexGameStateChangeEvent(game, BuiltInGameState.STARTED, BuiltInGameState.STARTED));
    }
}
