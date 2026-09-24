package me.mykindos.betterpvp.core;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Adds the libraries listed in {@code paper-libraries.txt} to Core's classpath. Paper downloads them from
 * Maven Central on first start and caches them in the server's {@code libraries} folder. Plugins that
 * depend on Core see these libraries through Core's classloader.
 */
@SuppressWarnings("UnstableApiUsage")
public class CoreLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("central", "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("paper-libraries.txt")) {
            if (stream == null) {
                throw new IllegalStateException("paper-libraries.txt is missing from the Core jar");
            }

            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .forEach(coordinates -> resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null)));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        classpathBuilder.addLibrary(resolver);
    }
}
