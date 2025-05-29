package me.mykindos.betterpvp.core.database.query;

import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StatementBuilderTest {

    private Statement.StatementBuilder builder;

    @BeforeEach
    void setUp() {
        builder = Statement.builder();
    }

    @Test
    void testInsertInto_SingleColumn() {
        Statement statement = builder
                .insertInto("users", "name")
                .values(new StringStatementValue("John"))
                .build();

        assertEquals("INSERT INTO users (name) VALUES (?)", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals("John", statement.getValues().get(0).getValue());
    }

    @Test
    void testInsertInto_MultipleColumns() {
        Statement statement = builder
                .insertInto("users", "id", "name", "age")
                .values(
                        new IntegerStatementValue(1),
                        new StringStatementValue("John"),
                        new IntegerStatementValue(25)
                )
                .build();

        assertEquals("INSERT INTO users (id, name, age) VALUES (?, ?, ?)", statement.getQuery());
        assertEquals(3, statement.getValues().size());
        assertEquals(1, statement.getValues().get(0).getValue());
        assertEquals("John", statement.getValues().get(1).getValue());
        assertEquals(25, statement.getValues().get(2).getValue());
    }

    @Test
    void testValuesBulk_SingleRow() {
        List<List<StatementValue<?>>> rows = List.of(
                List.of(
                        new IntegerStatementValue(1),
                        new StringStatementValue("John")
                )
        );

        Statement statement = builder
                .insertInto("users", "id", "name")
                .valuesBulk(rows)
                .build();

        assertEquals("INSERT INTO users (id, name) VALUES (?, ?)", statement.getQuery());
        assertEquals(2, statement.getValues().size());
    }

    @Test
    void testValuesBulk_MultipleRows() {
        List<List<StatementValue<?>>> rows = List.of(
                List.of(
                        new IntegerStatementValue(1),
                        new StringStatementValue("John")
                ),
                List.of(
                        new IntegerStatementValue(2),
                        new StringStatementValue("Jane")
                ),
                List.of(
                        new IntegerStatementValue(3),
                        new StringStatementValue("Bob")
                )
        );

        Statement statement = builder
                .insertInto("users", "id", "name")
                .valuesBulk(rows)
                .build();

        assertEquals("INSERT INTO users (id, name) VALUES (?, ?), (?, ?), (?, ?)", statement.getQuery());
        assertEquals(6, statement.getValues().size());

        // Verify values are in correct order
        assertEquals(1, statement.getValues().get(0).getValue());
        assertEquals("John", statement.getValues().get(1).getValue());
        assertEquals(2, statement.getValues().get(2).getValue());
        assertEquals("Jane", statement.getValues().get(3).getValue());
        assertEquals(3, statement.getValues().get(4).getValue());
        assertEquals("Bob", statement.getValues().get(5).getValue());
    }

    @Test
    void testValuesBulk_ComplexDataTypes() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        List<List<StatementValue<?>>> rows = List.of(
                List.of(
                        new UuidStatementValue(uuid1),
                        new StringStatementValue("Server1"),
                        new IntegerStatementValue(100)
                ),
                List.of(
                        new UuidStatementValue(uuid2),
                        new StringStatementValue("Server2"),
                        new IntegerStatementValue(200)
                )
        );

        Statement statement = builder
                .insertInto("logs", "id", "server", "value")
                .valuesBulk(rows)
                .build();

        assertEquals("INSERT INTO logs (id, server, value) VALUES (?, ?, ?), (?, ?, ?)", statement.getQuery());
        assertEquals(6, statement.getValues().size());
        assertEquals(uuid1.toString(), statement.getValues().get(0).getValue().toString());
        assertEquals("Server1", statement.getValues().get(1).getValue());
        assertEquals(100, statement.getValues().get(2).getValue());
        assertEquals(uuid2.toString(), statement.getValues().get(3).getValue().toString());
        assertEquals("Server2", statement.getValues().get(4).getValue());
        assertEquals(200, statement.getValues().get(5).getValue());
    }

    @Test
    void testValuesArray_SingleRow() {
        Statement statement = builder
                .insertInto("users", "id", "name")
                .values(
                        new IntegerStatementValue(1),
                        new StringStatementValue("John")
                )
                .build();

        assertEquals("INSERT INTO users (id, name) VALUES (?, ?)", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(1, statement.getValues().get(0).getValue());
        assertEquals("John", statement.getValues().get(1).getValue());
    }

    @Test
    void testValuesList_SingleRow() {
        List<StatementValue<?>> values = List.of(
                new IntegerStatementValue(1),
                new StringStatementValue("John")
        );

        Statement statement = builder
                .insertInto("users", "id", "name")
                .values(values)
                .build();

        assertEquals("INSERT INTO users (id, name) VALUES (?, ?)", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(1, statement.getValues().get(0).getValue());
        assertEquals("John", statement.getValues().get(1).getValue());
    }

    @Test
    void testValuesBulk_EmptyRows_ThrowsException() {
        List<List<StatementValue<?>>> emptyRows = new ArrayList<>();

        assertThrows(IllegalArgumentException.class, () -> {
            builder.insertInto("users", "id", "name")
                    .valuesBulk(emptyRows);
        });
    }

    @Test
    void testValuesBulk_InconsistentRowSizes_ThrowsException() {
        List<List<StatementValue<?>>> inconsistentRows = List.of(
                List.of(
                        new IntegerStatementValue(1),
                        new StringStatementValue("John")
                ),
                List.of(
                        new IntegerStatementValue(2) // Missing second value
                )
        );

        assertThrows(IllegalArgumentException.class, () -> {
            builder.insertInto("users", "id", "name")
                    .valuesBulk(inconsistentRows);
        });
    }

    @Test
    void testValuesBulk_LargeDataSet() {
        List<List<StatementValue<?>>> largeRows = new ArrayList<>();

        // Create 1000 rows
        for (int i = 0; i < 1000; i++) {
            largeRows.add(List.of(
                    new IntegerStatementValue(i),
                    new StringStatementValue("User" + i)
            ));
        }

        Statement statement = builder
                .insertInto("users", "id", "name")
                .valuesBulk(largeRows)
                .build();

        // Verify query structure
        assertTrue(statement.getQuery().startsWith("INSERT INTO users (id, name) VALUES"));
        assertTrue(statement.getQuery().contains("(?, ?)"));

        // Should have 1000 occurrences of "(?, ?)" separated by commas
        long placeholderCount = statement.getQuery().chars().filter(ch -> ch == '?').count();
        assertEquals(2000, placeholderCount); // 2 placeholders per row * 1000 rows

        // Verify values count
        assertEquals(2000, statement.getValues().size());

        // Verify first and last values
        assertEquals(0, statement.getValues().get(0).getValue());
        assertEquals("User0", statement.getValues().get(1).getValue());
        assertEquals(999, statement.getValues().get(1998).getValue());
        assertEquals("User999", statement.getValues().get(1999).getValue());
    }

    @Test
    void testInsertInto_NoColumns_EmptyParentheses() {
        Statement statement = builder
                .insertInto("users")
                .values()
                .build();

        assertEquals("INSERT INTO users () VALUES ()", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    void testMethodChaining() {
        // Test that all methods return the builder for chaining
        Statement.StatementBuilder result = builder
                .insertInto("users", "id", "name")
                .values(new IntegerStatementValue(1), new StringStatementValue("John"));

        assertSame(builder, result);
    }

    @Test
    void testValuesBulk_NullValues() {
        List<List<StatementValue<?>>> rows = List.of(
                List.of(
                        new IntegerStatementValue(1),
                        new StringStatementValue(null) // null value
                ),
                List.of(
                        new IntegerStatementValue(2),
                        new StringStatementValue("Jane")
                )
        );

        Statement statement = builder
                .insertInto("users", "id", "name")
                .valuesBulk(rows)
                .build();

        assertEquals("INSERT INTO users (id, name) VALUES (?, ?), (?, ?)", statement.getQuery());
        assertEquals(4, statement.getValues().size());
        assertEquals(1, statement.getValues().get(0).getValue());
        assertNull(statement.getValues().get(1).getValue());
        assertEquals(2, statement.getValues().get(2).getValue());
        assertEquals("Jane", statement.getValues().get(3).getValue());
    }


    @Test
    public void testJoin_InnerJoin_AppendsJoinClause() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM orders")
                .join("customers", Statement.JoinType.INNER, "c", "orders.customer_id", "c.id");

        Statement statement = builder.build();
        assertEquals("SELECT * FROM orders INNER JOIN customers c ON orders.customer_id = c.id", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testJoin_LeftJoin_AppendsJoinClause() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM orders")
                .join("payments", Statement.JoinType.LEFT, "p", "orders.payment_id", "p.id");

        Statement statement = builder.build();
        assertEquals("SELECT * FROM orders LEFT JOIN payments p ON orders.payment_id = p.id", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testJoin_MultipleJoins_AppendsWithCorrectFormatting() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM orders")
                .join("customers", Statement.JoinType.INNER, "c", "orders.customer_id", "c.id")
                .join("payments", Statement.JoinType.LEFT, "p", "orders.payment_id", "p.id");

        Statement statement = builder.build();
        assertEquals("SELECT * FROM orders INNER JOIN customers c ON orders.customer_id = c.id LEFT JOIN payments p ON orders.payment_id = p.id", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testWhere_FirstCondition_AppendsWhereClause() {
        Statement.StatementBuilder builder = Statement.builder();
        String column = "id";
        String operator = "=";
        StatementValue<Integer> value = IntegerStatementValue.of(1);

        builder.queryBase("SELECT * FROM users").where(column, operator, value);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users WHERE id = ?", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(value, statement.getValues().getFirst());
    }

    @Test
    public void testWhere_SecondCondition_AppendsAndClause() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<Integer> firstValue = IntegerStatementValue.of(1);
        StatementValue<String> secondValue = StringStatementValue.of("admin");

        builder.queryBase("SELECT * FROM users")
                .where("id", "=", firstValue)
                .where("role", "=", secondValue);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users WHERE id = ? AND role = ?", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(firstValue, statement.getValues().get(0));
        assertEquals(secondValue, statement.getValues().get(1));
    }

    @Test
    public void testWhere_MultipleConditions_AppendsWithCorrectFormatting() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<Integer> firstValue = IntegerStatementValue.of(1);
        StatementValue<String> secondValue = StringStatementValue.of("manager");
        StatementValue<Boolean> thirdValue = BooleanStatementValue.of(true);

        builder.queryBase("SELECT * FROM employees")
                .where("id", "=", firstValue)
                .where("role", "=", secondValue)
                .where("active", "=", thirdValue);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM employees WHERE id = ? AND role = ? AND active = ?", statement.getQuery());
        assertEquals(3, statement.getValues().size());
        assertEquals(firstValue, statement.getValues().get(0));
        assertEquals(secondValue, statement.getValues().get(1));
        assertEquals(thirdValue, statement.getValues().get(2));
    }

    @Test
    public void testWhere_BuildsQueryWithInitialWhereClauseOnlyOnce() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<String> firstValue = StringStatementValue.of("user1");

        builder.queryBase("SELECT * FROM accounts").where("username", "=", firstValue);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM accounts WHERE username = ?", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(firstValue, statement.getValues().getFirst());
    }

    @Test
    public void testWhere_NoValuesInitially_AddsWhereClauseCorrectly() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<Double> value = DoubleStatementValue.of(50.75);

        builder.queryBase("SELECT * FROM products").where("price", ">", value);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM products WHERE price > ?", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(value, statement.getValues().getFirst());
    }


    @Test
    public void testOrderBy_SingleColumnAscending() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM users").orderBy("username", true);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users ORDER BY username ASC", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testOrderBy_SingleColumnDescending() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM users").orderBy("registration_date", false);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users ORDER BY registration_date DESC", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testOrderBy_MultipleColumns() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM users")
                .orderBy("last_login", false)
                .orderBy("username", true);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users ORDER BY last_login DESC, username ASC", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testGroupBy_SingleColumn_AppendsGroupByClause() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT role, COUNT(*)")
                .groupBy("role");

        Statement statement = builder.build();
        assertEquals("SELECT role, COUNT(*) GROUP BY role", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testGroupBy_MultipleColumns_AppendsWithCorrectFormatting() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT department, role, COUNT(*)")
                .groupBy("department")
                .groupBy("role");

        Statement statement = builder.build();
        assertEquals("SELECT department, role, COUNT(*) GROUP BY department, role", statement.getQuery());
        assertEquals(0, statement.getValues().size());
    }

    @Test
    public void testGroupBy_CombinesWithOtherClauses() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<String> roleValue = StringStatementValue.of("manager");

        builder.queryBase("SELECT department, role, COUNT(*)")
                .where("role", "=", roleValue)
                .groupBy("department")
                .orderBy("department", true);

        Statement statement = builder.build();
        assertEquals("SELECT department, role, COUNT(*) WHERE role = ? GROUP BY department ORDER BY department ASC", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(roleValue, statement.getValues().get(0));
    }

    @Test
    public void testLimit_AddsLimitClauseWithCorrectValue() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM users").limit(10);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users LIMIT ?", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(IntegerStatementValue.of(10), statement.getValues().get(0));
    }

    @Test
    public void testLimit_CombinesWithOtherClauses() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<String> roleValue = StringStatementValue.of("admin");

        builder.queryBase("SELECT * FROM employees")
                .where("role", "=", roleValue)
                .orderBy("id", true)
                .limit(5);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM employees WHERE role = ? ORDER BY id ASC LIMIT ?", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(roleValue, statement.getValues().get(0));
        assertEquals(IntegerStatementValue.of(5), statement.getValues().get(1));
    }

    @Test
    public void testOffset_AddsOffsetClauseWithCorrectValue() {
        Statement.StatementBuilder builder = Statement.builder();

        builder.queryBase("SELECT * FROM users").offset(10);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM users OFFSET ?", statement.getQuery());
        assertEquals(1, statement.getValues().size());
        assertEquals(IntegerStatementValue.of(10), statement.getValues().getFirst());
    }

    @Test
    public void testOffset_CombinesWithOtherClauses() {
        Statement.StatementBuilder builder = Statement.builder();
        StatementValue<String> roleValue = StringStatementValue.of("user");

        builder.queryBase("SELECT * FROM employees")
                .where("role", "=", roleValue)
                .orderBy("id", false)
                .offset(5);

        Statement statement = builder.build();
        assertEquals("SELECT * FROM employees WHERE role = ? ORDER BY id DESC OFFSET ?", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(roleValue, statement.getValues().get(0));
        assertEquals(IntegerStatementValue.of(5), statement.getValues().get(1));
    }

    @Test
    public void testWhereOr_FirstCondition_AppendsWhereClause() {
        Statement.StatementBuilder builder = Statement.builder();
        
        List<Statement.WhereCondition> conditions = new ArrayList<>();
        conditions.add(new Statement.WhereCondition("id", "=", IntegerStatementValue.of(1)));
        conditions.add(new Statement.WhereCondition("id", "=", IntegerStatementValue.of(2)));
        
        builder.queryBase("SELECT * FROM users").whereOr(conditions);
        
        Statement statement = builder.build();
        assertEquals("SELECT * FROM users WHERE (id = ? OR id = ?)", statement.getQuery());
        assertEquals(2, statement.getValues().size());
        assertEquals(IntegerStatementValue.of(1), statement.getValues().get(0));
        assertEquals(IntegerStatementValue.of(2), statement.getValues().get(1));
    }
    
    @Test
    public void testWhereOr_MultipleConditions_WithDifferentColumnsAndOperators() {
        Statement.StatementBuilder builder = Statement.builder();
        
        List<Statement.WhereCondition> conditions = new ArrayList<>();
        conditions.add(new Statement.WhereCondition("id", "=", IntegerStatementValue.of(1)));
        conditions.add(new Statement.WhereCondition("name", "LIKE", StringStatementValue.of("%John%")));
        conditions.add(new Statement.WhereCondition("age", ">", IntegerStatementValue.of(18)));
        
        builder.queryBase("SELECT * FROM users").whereOr(conditions);
        
        Statement statement = builder.build();
        assertEquals("SELECT * FROM users WHERE (id = ? OR name LIKE ? OR age > ?)", statement.getQuery());
        assertEquals(3, statement.getValues().size());
    }
    
    @Test
    public void testWhereOr_CombinesWithRegularWhere() {
        Statement.StatementBuilder builder = Statement.builder();
        
        StatementValue<Boolean> activeValue = BooleanStatementValue.of(true);
        
        List<Statement.WhereCondition> conditions = new ArrayList<>();
        conditions.add(new Statement.WhereCondition("name", "=", StringStatementValue.of("John")));
        conditions.add(new Statement.WhereCondition("name", "=", StringStatementValue.of("Jane")));
        
        builder.queryBase("SELECT * FROM users")
                .where("active", "=", activeValue)
                .whereOr(conditions);
        
        Statement statement = builder.build();
        assertEquals("SELECT * FROM users WHERE active = ? AND (name = ? OR name = ?)", statement.getQuery());
        assertEquals(3, statement.getValues().size());
        assertEquals(activeValue, statement.getValues().get(0));
    }
    
    @Test
    public void testWhereOrSameColumn_BuildsCorrectQuery() {
        Statement.StatementBuilder builder = Statement.builder();
        
        List<StatementValue<?>> statusValues = Arrays.asList(
                StringStatementValue.of("pending"),
                StringStatementValue.of("approved"),
                StringStatementValue.of("in_review")
        );
        
        builder.queryBase("SELECT * FROM orders").whereOrSameColumn("status", "=", statusValues);
        
        Statement statement = builder.build();
        assertEquals("SELECT * FROM orders WHERE (status = ? OR status = ? OR status = ?)", statement.getQuery());
        assertEquals(3, statement.getValues().size());
    }
    
    @Test
    public void testWhereOrEquals_BuildsCorrectQuery() {
        Statement.StatementBuilder builder = Statement.builder();
        
        List<StatementValue<?>> idValues = Arrays.asList(
                IntegerStatementValue.of(1),
                IntegerStatementValue.of(2),
                IntegerStatementValue.of(3)
        );
        
        builder.queryBase("SELECT * FROM products").whereOrEquals("category_id", idValues);
        
        Statement statement = builder.build();
        assertEquals("SELECT * FROM products WHERE (category_id = ? OR category_id = ? OR category_id = ?)", statement.getQuery());
        assertEquals(3, statement.getValues().size());
    }
    
}