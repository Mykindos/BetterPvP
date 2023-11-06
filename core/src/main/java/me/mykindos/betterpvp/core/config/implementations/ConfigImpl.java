package me.mykindos.betterpvp.core.config.implementations;

import me.mykindos.betterpvp.core.config.Config;
import org.apache.commons.lang3.AnnotationUtils;

import java.lang.annotation.Annotation;

@SuppressWarnings({"ClassExplicitlyAnnotation", "EqualsWhichDoesntCheckParameterClass"})
public record ConfigImpl(String path, String defaultValue) implements Config {

    @Override
    public int hashCode() {
        return AnnotationUtils.hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return AnnotationUtils.equals(this, (Annotation) o);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return Config.class;
    }
}
