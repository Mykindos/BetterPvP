package me.mykindos.betterpvp.game.util;

import lombok.CustomLog;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@UtilityClass
@CustomLog
public class UtilResource {

    /**
     * Unzips a file from resources to the destination folder.
     * @param destinationFolder The folder to extract the file to
     * @param name The name of the zip file in resources. The extension ".zip" is added automatically, so it should not be included in the name.
     * @return The extracted file
     * @throws IOException If an I/O error occurs
     */
    public static File unzipFile(File destinationFolder, String name) throws IOException {
        File worldDir = new File(Bukkit.getWorldContainer(), name);
        if (worldDir.exists()) {
            // World already extracted
            return worldDir;
        }

        // Get the zip file from resources
        InputStream zipStream = UtilResource.class.getClassLoader().getResourceAsStream(name + ".zip");
        if (zipStream == null) {
            throw new IOException("Lobby zip file not found in resources: " + name + ".zip");
        }

        // Extract zip file
        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            byte[] buffer = new byte[2048];

            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(Bukkit.getWorldContainer(), entry.getName());

                // Create directories if needed
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                    continue;
                }

                // Create parent directories if needed
                new File(newFile.getParent()).mkdirs();

                // Write file content
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }

                zis.closeEntry();
            }
        }

        log.info("Extracted zip to: {}", worldDir.getPath()).submit();
        return worldDir;
    }

}
