package me.mykindos.betterpvp.core.database.query;

import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatementBuilderTest {

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

}