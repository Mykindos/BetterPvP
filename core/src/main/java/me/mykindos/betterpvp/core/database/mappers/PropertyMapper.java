package me.mykindos.betterpvp.core.database.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import org.jooq.Record2;
import org.jooq.Result;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static me.mykindos.betterpvp.core.database.jooq.Tables.PROPERTY_MAP;

@CustomLog
@Singleton
public class PropertyMapper {

    private final Database database;
    private final Map<String, String> propertyMap;

    @Inject
    public PropertyMapper(Database database) {
        this.database = database;
        this.propertyMap = getPropertyMap();
    }

    public Map<String, String> getPropertyMap() {
        Map<String, String> map = new ConcurrentHashMap<>();
        database.getDslContext().selectFrom(PROPERTY_MAP).fetch().forEach(propertyRecord -> {
            map.put(propertyRecord.get(PROPERTY_MAP.PROPERTY), propertyRecord.get(PROPERTY_MAP.TYPE));
        });

        return map;
    }

    /**
     *
     * @param result A CachedRowSet with 2 columns, Property and Value
     * @param container The property container that the properties will be added to
     */
    public void parseProperties(Result<Record2<String, String>> result, PropertyContainer container) {

        result.forEach(propertyRecord -> {
            String property = propertyRecord.component1();
            String type = propertyMap.get(property);

            if(type == null){
                log.error("Property type not found for property: " + property).submit();
                return;
            }

            Object value = switch (type.toLowerCase()) {
                case "int" -> Integer.parseInt(propertyRecord.component2());
                case "boolean" -> Boolean.parseBoolean(propertyRecord.component2());
                case "double" -> Double.parseDouble(propertyRecord.component2());
                case "long" -> Long.parseLong(propertyRecord.component2());
                case "string" -> propertyRecord.component2();
                default -> {
                    try {
                        yield Class.forName(type).cast(propertyRecord.component2());
                    } catch (ClassNotFoundException e) {
                        log.error("Failed to parse property {} to class {}", propertyRecord.component2(), type, e).submit();
                        yield null;
                    }
                }
            };

            container.putProperty(property, value, true);
        });


    }

    public void parseProperty(String property, String value, PropertyContainer container) throws ClassNotFoundException {

        String type = propertyMap.get(property);

        if(type == null){
            log.error("Property type not found for property: " + property).submit();
            return;
        }

        Object convertedValue = switch (type.toLowerCase()) {
            case "int" -> Integer.valueOf(value);
            case "boolean" -> Boolean.parseBoolean(value);
            case "double" -> Double.parseDouble(value);
            case "long" -> Long.parseLong(value);
            case "string" -> value;
            default -> Class.forName(type).cast(value);
        };

        container.putProperty(property, convertedValue, true);
    }
}
