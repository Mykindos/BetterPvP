package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.Config;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Singleton
@Slf4j
@Getter
public class ResourcePackHandler {

    @Inject
    @Config(path = "resourcepack.url", defaultValue = "")
    private String resourcePackUrl;

    @Inject
    @Config(path = "resourcepack.sha", defaultValue = "")
    private String resourcePackSha;

    @Inject
    @Config(path = "resourcepack.force", defaultValue = "false")
    private boolean forceResourcePack;

    @Inject
    @Config(path = "resourcepack.enabled", defaultValue = "false")
    private boolean resourcePackEnabled;

    @Inject
    @Config(path = "resourcepack.selfhost", defaultValue = "false")
    private boolean selfHosted;

    @Inject
    @Config(path="resourcepack.filepath", defaultValue = "pack.zip")
    private String resoucePackPath;

    private HttpServer selfHostedPackServer;
    private String cachedSha;


    private void createHttpServer() {
        try {
            selfHostedPackServer = HttpServer.create(new InetSocketAddress(8081), 0);
            selfHostedPackServer.createContext("/pack.zip", exchange -> {
                try {

                    Path filePath = Paths.get(resoucePackPath);
                    byte[] fileContent = Files.readAllBytes(filePath);
                    exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=" + filePath.getFileName().toString());
                    exchange.sendResponseHeaders(200, fileContent.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(fileContent);
                    os.close();
                }catch (Exception ex){
                    log.error("Failed to get resource pack", ex);
                    exchange.sendResponseHeaders(500, 0); // Send a 500 Internal Server Error response
                    exchange.close();
                }
            });
            selfHostedPackServer.setExecutor(null); // creates a default executor
            selfHostedPackServer.start();
        }catch (Exception ex) {
            log.error("Failed to create HttpServer for resource pack", ex);
        }
    }

    public String getResourcePackSha() {
        if(selfHosted) {
            if(selfHostedPackServer == null) {
                createHttpServer();
            }

            if(cachedSha == null) {
                cachedSha = calculateSHA1(Paths.get(resoucePackPath));
            }

            return cachedSha;
        }
        return resourcePackSha;
    }

    private String calculateSHA1(Path filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(filePath);
            MessageDigest shaDigest = MessageDigest.getInstance("SHA-1");
            byte[] hash = shaDigest.digest(fileContent);

            // Convert the byte to hex format
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reload() {
        this.cachedSha = null;
    }

}
