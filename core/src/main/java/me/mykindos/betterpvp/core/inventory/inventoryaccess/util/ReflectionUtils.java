package me.mykindos.betterpvp.core.inventory.inventoryaccess.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings({"unchecked", "unused"})
public class ReflectionUtils {

    
    public static <T> @NotNull Class<T> getCBClass(@NotNull String path) {
        return getClass(ReflectionRegistry.CRAFT_BUKKIT_PACKAGE_PATH + "." + path);
    }
    
    public static <T> @NotNull Class<T> getClass(@NotNull String path) {
        try {
            return (Class<T>) Class.forName(path);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <T> @Nullable Class<T> getClassOrNull(@NotNull String path) {
        try {
            return (Class<T>) Class.forName(path);
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static @NotNull Field getField(@NotNull Class<?> clazz, boolean declared, @NotNull String name) {
        try {
            Field field = declared ? clazz.getDeclaredField(name) : clazz.getField(name);
            if (declared) field.setAccessible(true);
            return field;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <T> @NotNull Constructor<T> getConstructor(@NotNull Class<T> clazz, boolean declared, @NotNull Class<?> @NotNull ... parameterTypes) {
        try {
            Constructor<T> constructor = declared ? clazz.getDeclaredConstructor(parameterTypes) : clazz.getConstructor(parameterTypes);
            if (declared) constructor.setAccessible(true);
            return constructor;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <T> @NotNull T constructEmpty(@NotNull Class<?> clazz) {
        try {
            return (T) getConstructor(clazz, true).newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static <T> @NotNull T construct(@NotNull Constructor<T> constructor, @Nullable Object @Nullable ... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static @NotNull Method getMethod(@NotNull Class<?> clazz, boolean declared, @NotNull String name, @NotNull Class<?> @NotNull ... parameterTypes) {
        return null; // TODO figure out reflection here with mojang mappings?
        //try {
        //    Method method = declared ? clazz.getDeclaredMethod(name, parameterTypes) : clazz.getMethod(name, parameterTypes);
        //    if (declared) method.setAccessible(true);
        //    return method;
        //} catch (Throwable t) {
        //    throw new RuntimeException(t);
        //}
    }
    
    public static @Nullable Method getMethodOrNull(@NotNull Class<?> clazz, boolean declared, @NotNull String name, @NotNull Class<?> @NotNull ... parameterTypes) {
        try {
            Method method = declared ? clazz.getDeclaredMethod(name, parameterTypes) : clazz.getMethod(name, parameterTypes);
            if (declared) method.setAccessible(true);
            return method;
        } catch (Exception ex) {
            return null;
        }
    }
    
    public static <T> T invokeMethod(@NotNull Method method, @Nullable Object obj, @Nullable Object @Nullable ... args) {
        try {
            return (T) method.invoke(obj, args);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static void setFieldValue(@NotNull Field field, @Nullable Object obj, @Nullable Object value) {
        try {
            field.set(obj, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T getFieldValue(@NotNull Field field, @Nullable Object obj) {
        try {
            return (T) field.get(obj);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
