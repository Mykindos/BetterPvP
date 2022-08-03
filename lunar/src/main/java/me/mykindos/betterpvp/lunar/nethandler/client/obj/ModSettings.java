package me.mykindos.betterpvp.lunar.nethandler.client.obj;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.mykindos.betterpvp.lunar.nethandler.ModSettingsAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ModSettings {

    public static final Gson GSON = new GsonBuilder().registerTypeHierarchyAdapter(ModSettings.class, new ModSettingsAdapter()).create();

    private Map<String, ModSetting> modSettings = new HashMap<>();

    public ModSettings addModSetting(String modId, ModSetting setting) {
        modSettings.put(modId, setting);
        return this;
    }

    public ModSetting getModSetting(String modId) {
        return this.modSettings.get(modId);
    }

    public Map<String, ModSetting> getModSettings() {
        return modSettings;
    }

    public static class ModSetting {
        private boolean enabled;
        private Map<String, Object> properties;

        public ModSetting() { } // for serialization

        public ModSetting(boolean enabled, Map<String, Object> properties) {
            this.enabled = enabled;
            this.properties = properties;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public String toString() {
            return "ModSetting{" +
                    "enabled=" + enabled +
                    ", properties=" + properties +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModSetting that = (ModSetting) o;
            return enabled == that.enabled &&
                    Objects.equals(properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, properties);
        }
    }

}
