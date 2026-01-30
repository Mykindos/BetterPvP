package me.mykindos.betterpvp.core.resourcepack;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.resourcepack.exceptions.ResourcePackShaException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
public class DefaultResourcePackLoader implements IResourcePackLoader {

    private final Core core;

    public DefaultResourcePackLoader(Core core) {
        this.core = core;
    }

    @Override
    public CompletableFuture<ResourcePack> loadResourcePack(String name) {
        return CompletableFuture.supplyAsync(() -> {
            ExtendedYamlConfiguration resourcepacks = core.getConfig("resourcepacks");

            String url = resourcepacks.getString("packs." + name + ".url");
            if(url == null) return null;

            String hash = resourcepacks.getString("packs." + name + ".hash");
            if(hash == null) {
                hash = calculateSHA1(url);
            }

            UUID uuid = UUID.nameUUIDFromBytes(url.getBytes(StandardCharsets.UTF_8));

            return new ResourcePack(uuid, url, hash);
        });
    }

    private String calculateSHA1(String fileUrl) {
        try {
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = new URI(fileUrl).toURL().openStream()) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    shaDigest.update(buffer, 0, read);
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
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
