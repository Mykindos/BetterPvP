package me.mykindos.betterpvp.core.locale;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

final class UTF8ResourceBundleControl extends ResourceBundle.Control {

    static final UTF8ResourceBundleControl INSTANCE = new UTF8ResourceBundleControl();

    private UTF8ResourceBundleControl() {
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {
        if (!"java.properties".equals(format)) {
            return null;
        }

        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, "properties");

        InputStream stream;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            stream = connection.getInputStream();
        } else {
            stream = loader.getResourceAsStream(resourceName);
        }

        if (stream == null) {
            return null;
        }

        try (stream; var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
        }
    }
}