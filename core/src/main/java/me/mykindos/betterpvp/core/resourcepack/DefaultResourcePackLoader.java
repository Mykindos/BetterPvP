package me.mykindos.betterpvp.core.resourcepack;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.resourcepack.exceptions.ResourcePackShaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@CustomLog
public class DefaultResourcePackLoader implements IResourcePackLoader {

    private final Core core;

    public DefaultResourcePackLoader(Core core) {
        this.core = core;
    }

    @Override
    public ResourcePack loadResourcePack(String name) {
        ExtendedYamlConfiguration resourcepacks = core.getConfig("resourcepacks");

        String url = resourcepacks.getString("packs." + name + ".url");
        if(url == null) return null;

        String hash = resourcepacks.getString("packs." + name + ".hash");
        if(hash == null) {
            hash = calculateSHA1(url);
        }

        UUID uuid = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));

        return new ResourcePack(uuid, url, hash);
    }

    private String calculateSHA1(String fileUrl) {
        try {
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = new URL(fileUrl).openStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    shaDigest.update(buffer, 0, read);
                }
            }
            byte[] hash = shaDigest.digest();

            // Convert the byte to hex format
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to get SHA for " + fileUrl, e);
            throw new ResourcePackShaException("Failed to get SHA for " + fileUrl);
        }
    }
}
