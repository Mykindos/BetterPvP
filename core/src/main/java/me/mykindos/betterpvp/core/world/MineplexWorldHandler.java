package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
        if (UtilWorld.getUnloadedWorlds().stream().noneMatch(file -> file.getName().equalsIgnoreCase("world"))) {
            log.info("World folder not found. Creating...").submit();

            try {
                // Loop through all zips in the assets/worlds folder
                for (File file : new File(rootDir + "/assets/worlds").listFiles()) {
                    if (file.getName().endsWith(".zip")) {
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
        }
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
            if (!entry.isDirectory()) {
                extractFile(zis, filePath);
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
}
