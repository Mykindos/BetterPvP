package me.mykindos.betterpvp.core.database.query;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Statement {

    /**
     * Represents the SQL query string associated with the {@code Statement} or {@code StatementBuilder}.
     * This variable serves as the core construct for creating, modifying, and executing database queries.
     * The query string is dynamically built or directly provided depending on the context in which
     * the {@code Statement} or {@code StatementBuilder} is used.
     */
    private String query;

    /**
     * A protected list that stores instances of {@code StatementValue<?>}. This list represents
     * the values associated with a SQL statement. Each value encapsulates a parameter and its
     * corresponding SQL type, which can be used for dynamically building or executing
     * parameterized SQL queries. The values are initialized as an empty {@code ArrayList}.
     */
    protected List<StatementValue<?>> values = new ArrayList<>();
    /**
     * Indicates whether a SQL WHERE clause has been included in the statement.
     * This flag is used to ensure that subsequent conditions in the query are
     * appended correctly, with proper syntax (e.g., adding "AND" after the initial
     * WHERE clause instead of starting a new WHERE clause).
     */
    private boolean hasWhere;
    /**
     * Indicates whether the SQL query being constructed includes an "ORDER BY" clause.
     * This flag is used internally to track if an "ORDER BY" statement has been appended
     * to the query string, ensuring proper formatting and preventing duplicate clauses.
     */
    private boolean hasOrderBy;
    /**
     * Indicates whether the SQL statement includes a GROUP BY clause.
     * This flag is set to true if at least one GROUP BY operation is added
     * to the statement during its construction.
     */
    private boolean hasGroupBy;
    /**
     * Indicates whether a LIMIT clause has been added to the SQL statement being constructed.
     * This variable is used to track the inclusion of a LIMIT clause in the query.
     * When set to {@code true}, it signifies that a LIMIT clause is present in the current SQL query.
     */
    private boolean hasLimit;
    /**
     * Represents whether the current SQL statement includes an OFFSET clause.
     *
     * The OFFSET clause in an SQL query is used to skip a specific number of rows
     * before returning the result set. It is typically used in combination with the
     * LIMIT clause for pagination purposes.
     *
     * This variable is primarily managed internally within the {@code StatementBuilder}
     * class to track the presence of the OFFSET clause during the construction of an
     * SQL query. It ensures that the appropriate SQL syntax and parameters are included
     * when the OFFSET clause is added to the query.
     */
    private boolean hasOffset;
    /**
     * Indicates whether the SQL statement includes a FORCE INDEX clause.
     * When set to {@code true}, it signifies that a specific index has been forced
     * to be used by the query optimizer.
     */
    private boolean hasForceIndex;
    /**
     * Indicates whether the SQL INSERT statement should use the LOW_PRIORITY modifier.
     * When set to {@code true}, the INSERT statement will include the LOW_PRIORITY keyword,
     * which tells the database to delay the insert until no clients are reading from the table.
     */
    private boolean isLowPriority;

    /**
     * Constructs a new Statement object with a given SQL query and optional parameterized values.
     *
     * @param query the SQL query string to be executed
     * @param values optional array of parameterized {@code StatementValue} objects representing the values to bind
     *               to the query placeholders
     */
    public Statement(String query, StatementValue<?>... values) {
        this.query = query;
        if (values.length > 0) {
            this.values = Arrays.asList(values);
        }
    }

    /**
     * Constructs a new SQL statement with the given query and values.
     *
     * @param query the SQL query string to be executed; must not be null.
     * @param values a list of {@link StatementValue} objects representing the
     *               parameterized values to be used in the query; must not be null.
     */
    public Statement(String query, List<StatementValue<?>> values) {
        this.query = query;
        this.values = values;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum JoinType {
        INNER("INNER"),
        LEFT("LEFT"),
        RIGHT("RIGHT"),
        FULL("FULL");

        private final String sqlKeyword;
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public enum SortOrder {
        ASCENDING("ASC"),
        DESCENDING("DESC");

        private final String sqlKeyword;
    }


    /**
     * A utility class for building SQL statement queries dynamically. The class provides methods
     * for constructing various SQL query components such as SELECT, UPDATE, DELETE, JOIN,
     * WHERE, ORDER BY, GROUP BY, LIMIT, and OFFSET clauses. The resulting query and its associated
     * parameters can be retrieved and executed using appropriate database operations.
     */
    /**
     * Helper class to represent a WHERE condition with a column, operator, and value
     */
    public static class WhereCondition {
        private final String column;
        private final String operator;
        private final StatementValue<?> value;

        public WhereCondition(String column, String operator, StatementValue<?> value) {
            this.column = column;
            this.operator = operator;
            this.value = value;
        }
    }

    public static class StatementBuilder {
        private static final String SELECT = "SELECT ";
        private static final String FROM = " FROM ";
        private static final String UPDATE = "UPDATE ";
        private static final String DELETE_FROM = "DELETE FROM ";
        private static final String JOIN = " JOIN ";
        private static final String ON = " ON ";
        private static final String WHERE = " WHERE ";
        private static final String AND = " AND ";
        private static final String OR = " OR ";
        private static final String ORDER_BY = " ORDER BY ";
        private static final String GROUP_BY = " GROUP BY ";
        private static final String LIMIT = " LIMIT ?";
        private static final String OFFSET = " OFFSET ?";
        private static final String COMMA = ", ";
        private static final String EQUALS = " = ";
        private static final String PARAMETER = " ?";

        private String query;
        private List<StatementValue<?>> values;
        private boolean hasWhere;
        private boolean hasOrderBy;
        private boolean hasGroupBy;
        private boolean hasLimit;
        private boolean hasOffset;
        private boolean hasForceIndex;
        private boolean isLowPriority;

        public StatementBuilder() {
            this.values = new ArrayList<>();
        }

        /**
         * Sets the LOW_PRIORITY flag for INSERT operations.
         * When this flag is set, the INSERT statement will include the LOW_PRIORITY modifier,
         * which tells the database to delay the insert until no clients are reading from the table.
         * 
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder lowPriority() {
            this.isLowPriority = true;
            return this;
        }

        /**
         * Sets the base query string to construct an SQL statement and initializes the query building process.
         *
         * @param base the base query string, usually the starting portion of an SQL statement
         *             (e.g., "SELECT * FROM table" or "UPDATE table").
         * @return the current instance of {@code StatementBuilder} to allow method chaining.
         */
        public StatementBuilder queryBase(String base) {
            this.query = base;
            return this;
        }

        /**
         * Adds a FORCE INDEX clause to the SQL query to influence the query optimizer to use a specific index.
         * This method is typically used after a FROM clause and before WHERE, JOIN, or other clauses.
         *
         * @param indexName the name of the index to force the query optimizer to use
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder forceIndex(String indexName) {
            if (!this.hasForceIndex) {
                this.query += " FORCE INDEX (" + indexName + ")";
                this.hasForceIndex = true;
            }
            return this;
        }

        /**
         * Constructs a SQL SELECT query statement using the specified table and columns.
         * This method initializes the query string with the SELECT and FROM clauses.
         *
         * @param table the name of the table to select data from
         * @param columns the column names to include in the SELECT statement
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder select(String table, String... columns) {
            this.query = SELECT + String.join(COMMA, columns) + FROM + table;
            return this;
        }

        /**
         * Initializes an SQL UPDATE statement by setting the target table name.
         * Modifies the internal query to begin with the UPDATE clause.
         *
         * @param table the name of the table to update
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder update(String table) {
            this.query = UPDATE + table;
            return this;
        }

        /**
         * Constructs an SQL DELETE statement for the specified table.
         * This method sets the query to begin with the DELETE FROM clause.
         *
         * @param table the name of the table from which records should be deleted
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder delete(String table) {
            this.query = DELETE_FROM + table;
            return this;
        }

        /**
         * Adds a join clause to the SQL query being constructed. This method supports different types of joins
         * (e.g., INNER, LEFT, RIGHT, FULL) and allows specifying an alias for the joined table, along with the
         * column names to use for the join condition.
         *
         * @param table the name of the table to join with
         * @param joinType the type of join to perform (e.g., INNER, LEFT, RIGHT, FULL)
         * @param alias the alias to use for the joined table in the query
         * @param column1 the column name from the current table to use in the join condition
         * @param column2 the column name from the joined table to use in the join condition
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder join(String table, JoinType joinType, String alias, String column1, String column2) {
            this.query += " " + joinType.getSqlKeyword() + JOIN + table + " " + alias + ON + column1 + EQUALS + column2;
            return this;
        }

        /**
         * Adds a conditional clause to the SQL query being built. If this is the first condition
         * being added, it appends a WHERE clause to the query. For subsequent conditions, it appends
         * the condition using an AND operator.
         *
         * @param column the column name to be used in the condition
         * @param operator the SQL operator to be applied in the condition (e.g., =, >, <, LIKE)
         * @param value the value to be matched against the column, wrapped in a {@code StatementValue<?>} object
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder where(String column, String operator, StatementValue<?> value) {
            if (!this.hasWhere) {
                this.query += WHERE + column + " " + operator + PARAMETER;
                this.hasWhere = true;
            } else {
                this.query += AND + column + " " + operator + PARAMETER;
            }
            values.add(value);
            return this;
        }

        /**
         * Adds an OR condition group to the SQL query. 
         * Creates SQL like: (column1 operator1 value1 OR column2 operator2 value2 OR ...)
         * If this is the first condition being added, it appends a WHERE clause.
         * For subsequent conditions, it appends using an AND operator.
         * 
         * @param conditions List of WhereCondition objects to combine with OR
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder whereOr(List<WhereCondition> conditions) {
            if (conditions == null || conditions.isEmpty()) {
                return this;
            }

            StringBuilder conditionBuilder = new StringBuilder("(");

            for (int i = 0; i < conditions.size(); i++) {
                WhereCondition condition = conditions.get(i);
                conditionBuilder.append(condition.column)
                        .append(" ")
                        .append(condition.operator)
                        .append(PARAMETER);

                if (i < conditions.size() - 1) {
                    conditionBuilder.append(OR);
                }

                values.add(condition.value);
            }

            conditionBuilder.append(")");

            if (!this.hasWhere) {
                this.query += WHERE + conditionBuilder;
                this.hasWhere = true;
            } else {
                this.query += AND + conditionBuilder;
            }

            return this;
        }

        /**
         * Convenience method for creating OR conditions on the same column with the same operator.
         * Creates SQL like: (column operator value1 OR column operator value2 OR ...)
         * 
         * @param column The column name to use for all conditions
         * @param operator The operator to use for all conditions
         * @param values List of values to compare with OR
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder whereOrSameColumn(String column, String operator, List<StatementValue<?>> values) {
            if (values == null || values.isEmpty()) {
                return this;
            }

            List<WhereCondition> conditions = new ArrayList<>();
            for (StatementValue<?> value : values) {
                conditions.add(new WhereCondition(column, operator, value));
            }

            return whereOr(conditions);
        }

        /**
         * Convenience method for creating equality OR conditions on the same column.
         * Creates SQL like: (column = value1 OR column = value2 OR ...)
         * 
         * @param column The column name to check equality for
         * @param values List of values to check equality with OR
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder whereOrEquals(String column, List<StatementValue<?>> values) {
            return whereOrSameColumn(column, "=", values);
        }



        /**
         * Adds an ORDER BY clause to the SQL query using the specified column and sort order.
         * If an ORDER BY clause has already been added, the new clause is appended with a comma separator.
         *
         * @param column the name of the column to order the results by
         * @param order the sort order for the column (e.g., ASCENDING, DESCENDING)
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder orderBy(String column, SortOrder order) {
            return appendClause(
                ORDER_BY, 
                column + " " + order.getSqlKeyword(), 
                COMMA, 
                () -> this.hasOrderBy, 
                () -> this.hasOrderBy = true
            );
        }

        /**
         * Adds an ORDER BY clause to the SQL query using the specified column and sort order.
         * If an ORDER BY clause has already been added, the new clause is appended with a comma separator.
         *
         * @param column the name of the column to order the results by
         * @param ascending a boolean indicating the sort order; {@code true} for ascending, {@code false} for descending
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder orderBy(String column, boolean ascending) {
            return orderBy(column, ascending ? SortOrder.ASCENDING : SortOrder.DESCENDING);
        }

        /**
         * Adds a GROUP BY clause to the SQL query using the specified column.
         * If a GROUP BY clause has already been added, the new column is appended with a comma separator.
         *
         * @param column the name of the column to group the results by
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder groupBy(String column) {
            return appendClause(
                GROUP_BY, 
                column, 
                COMMA, 
                () -> this.hasGroupBy, 
                () -> this.hasGroupBy = true
            );
        }

        /**
         * Adds a LIMIT clause to the SQL query, restricting the number of rows returned.
         * This method sets the upper limit of records to fetch in the result set.
         *
         * @param limit the maximum number of records to return
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder limit(int limit) {
            this.query += LIMIT;
            values.add(IntegerStatementValue.of(limit));
            hasLimit = true;
            return this;
        }

        /**
         * Adds an OFFSET clause to the SQL query, specifying the number of rows to skip before starting to return rows.
         *
         * @param offset the number of rows to skip
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder offset(int offset) {
            this.query += OFFSET;
            values.add(IntegerStatementValue.of(offset));
            hasOffset = true;
            return this;
        }

        /**
         * Constructs a bulk INSERT statement for inserting multiple rows into the specified table.
         * This method creates a single INSERT statement with multiple value sets for better performance.
         * If the LOW_PRIORITY flag is set, the INSERT statement will include the LOW_PRIORITY modifier,
         * which tells the database to delay the insert until no clients are reading from the table.
         *
         * @param table the name of the table to insert data into
         * @param columns the column names for the INSERT statement
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder insertInto(String table, String... columns) {
            String insertPrefix = this.isLowPriority ? "INSERT LOW_PRIORITY INTO " : "INSERT INTO ";
            this.query = insertPrefix + table + " (" + String.join(", ", columns) + ")";
            return this;
        }

        /**
         * Adds multiple value sets for a bulk INSERT operation.
         * Each inner list represents one row of values to be inserted.
         *
         * @param valueRows a list where each element is a list of StatementValue objects representing one row
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder valuesBulk(List<List<StatementValue<?>>> valueRows) {
            if (valueRows.isEmpty()) {
                throw new IllegalArgumentException("Value rows cannot be empty");
            }

            int columnCount = valueRows.get(0).size();
            StringBuilder valuesClause = new StringBuilder(" VALUES ");

            for (int i = 0; i < valueRows.size(); i++) {
                List<StatementValue<?>> row = valueRows.get(i);
                if (row.size() != columnCount) {
                    throw new IllegalArgumentException("All rows must have the same number of values");
                }

                if (i > 0) {
                    valuesClause.append(", ");
                }

                valuesClause.append("(");
                for (int j = 0; j < row.size(); j++) {
                    if (j > 0) {
                        valuesClause.append(", ");
                    }
                    valuesClause.append("?");
                    this.values.add(row.get(j));
                }
                valuesClause.append(")");
            }

            this.query += valuesClause.toString();
            return this;
        }

        /**
         * Convenience method for adding a single row of values.
         * Equivalent to calling valuesBulk with a single-element list.
         *
         * @param values the StatementValue objects for this row
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder values(StatementValue<?>... values) {
            return valuesBulk(List.of(List.of(values)));
        }

        /**
         * Convenience method for adding a single row of values from a list.
         *
         * @param values the list of StatementValue objects for this row
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        public StatementBuilder values(List<StatementValue<?>> values) {
            return valuesBulk(List.of(values));
        }


        /**
         * Appends a specific clause to the SQL query being built.
         * If the clause does not already exist, it prefixes the clause with a specified string
         * and marks the clause as set using the provided flag setter.
         * Otherwise, it appends the clause content using the specified separator.
         *
         * @param clausePrefix the prefix to add before the clause when it is appended for the first time
         * @param clauseContent the content of the clause to be appended (e.g., a column name or condition)
         * @param separator the separator to use if the clause already exists and new content is appended
         * @param hasClauseCheck a BooleanSupplier that checks whether the clause has already been added
         * @param setClauseFlag a Runnable that sets the flag indicating the clause has been added
         * @return the current instance of {@code StatementBuilder}, allowing for method chaining
         */
        private StatementBuilder appendClause(
                String clausePrefix, 
                String clauseContent, 
                String separator,
                java.util.function.BooleanSupplier hasClauseCheck, 
                Runnable setClauseFlag) {

            if (!hasClauseCheck.getAsBoolean()) {
                this.query += clausePrefix + clauseContent;
                setClauseFlag.run();
            } else {
                this.query += separator + clauseContent;
            }
            return this;
        }
    }
}
