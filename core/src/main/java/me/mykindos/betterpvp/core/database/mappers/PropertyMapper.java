package me.mykindos.betterpvp.core.database.mappers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.properties.PropertyContainer;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        String query = "SELECT * FROM property_map;";
        CachedRowSet result = database.executeQuery(new Statement(query), TargetDatabase.GLOBAL);
        Map<String, String> map = new ConcurrentHashMap<>();
        try {
            while (result.next()) {
                map.put(result.getString(1), result.getString(2));
            }
        } catch (SQLException ex) {
            log.error("Error loading property map", ex);
        }

        return map;
    }

    /**
     *
     * @param result A CachedRowSet with 2 columns, Property and Value
     * @param container The property container that the properties will be added to
     * @throws SQLException If the result set is invalid
     * @throws ClassNotFoundException If the property type is not found
     */
    public void parseProperties(CachedRowSet result, PropertyContainer container) throws SQLException, ClassNotFoundException {

        while (result.next()) {
            String property = result.getString(1);
            String type = propertyMap.get(property);

            if(type == null){
                log.error("Property type not found for property: " + property).submit();
                continue;
            }

            Object value = switch (type.toLowerCase()) {
                case "int" -> result.getInt(2);
                case "boolean" -> Boolean.parseBoolean(result.getString(2));
                case "double" -> Double.parseDouble(result.getString(2));
                case "long" -> Long.parseLong(result.getString(2));
                case "string" -> result.getString(2);
                default -> Class.forName(type).cast(result.getObject(2));
            };

            container.putProperty(property, value, true);
        }

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
