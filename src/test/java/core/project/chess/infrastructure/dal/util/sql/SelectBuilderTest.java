package core.project.chess.infrastructure.dal.util.sql;

import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectBuilderTest {

    private static int passesTests = 0;

    @Test
    void test() {
        assertEquals("SELECT name, age FROM users WHERE age > 18 ORDER BY age DESC LIMIT 10 OFFSET 5 ", select()
                .columns("name", "age")
                .from("users")
                .where("age > 18")
                .orderBy("age", Order.DESC)
                .limitAndOffset(10, 5));

        log();

        assertEquals("SELECT t.id AS token_id, t.token AS token, t.is_confirmed AS token_confirmation, t.creation_date AS token_creation_date, u.id AS id, u.username AS username, u.email AS email, u.password AS password, u.user_role AS user_role, u.rating AS rating, u.rating_deviation AS rating_deviation, u.rating_volatility AS rating_volatility, u.is_enable AS is_enable, u.creation_date AS creation_date, u.last_updated_date AS last_updated_date FROM UserToken t INNER JOIN UserAccount u ON t.user_id = u.id WHERE t.token = ? ",
                select()
                .column("t.id").as("token_id")
                .column("t.token").as("token")
                .column("t.is_confirmed").as("token_confirmation")
                .column("t.creation_date").as("token_creation_date")
                .column("u.id").as("id")
                .column("u.username").as("username")
                .column("u.email").as("email")
                .column("u.password").as("password")
                .column("u.user_role").as("user_role")
                .column("u.rating").as("rating")
                .column("u.rating_deviation").as("rating_deviation")
                .column("u.rating_volatility").as("rating_volatility")
                .column("u.is_enable").as("is_enable")
                .column("u.creation_date").as("creation_date")
                .column("u.last_updated_date").as("last_updated_date")
                .from("UserToken t")
                .innerJoin("UserAccount u", "t.user_id = u.id")
                .where("t.token = ?")
                .build());

        log();

        assertEquals("SELECT DISTINCT name FROM customers WHERE city = 'New York' ORDER BY name ", selectDistinct()
                .column("name")
                .from("customers")
                .where("city = 'New York'")
                .orderBy("name")
                .build());

        log();

        assertEquals("SELECT COUNT(id) FROM orders WHERE status = 'shipped' AND total > 1000 ", select()
                .count("id")
                .from("orders")
                .where("status = 'shipped'")
                .and("total > 1000")
                .build());

        log();

        assertEquals("SELECT name , CONCAT(first_name, last_name) AS full_name FROM employees ", select()
                .columns("name")
                .concat("first_name", "last_name")
                .as("full_name")
                .from("employees")
                .build());

        log();

        assertEquals("SELECT name , CONCAT(first_name, last_name) AS full_name FROM employees ", select()
                .column("name")
                .concat("first_name", "last_name")
                .as("full_name")
                .from("employees")
                .build());

        log();

        assertEquals("SELECT * FROM products WHERE price > 100 ORDER BY price DESC LIMIT 5 OFFSET 0 ", select()
                .all()
                .from("products")
                .where("price > 100")
                .orderBy("price", Order.DESC)
                .limitAndOffset(5, 0));

        log();

        assertEquals("SELECT name, age FROM users WHERE age BETWEEN 18 AND 30 ", select()
                .columns("name", "age")
                .from("users")
                .where("age BETWEEN 18 AND 30")
                .build());

        log();

        assertEquals("WITH recent_orders AS (SELECT id FROM orders WHERE created_at > '2024-01-01') SELECT * FROM recent_orders ",
                withAndSelect("recent_orders", "SELECT id FROM orders WHERE created_at > '2024-01-01'")
                .columns("*")
                .from("recent_orders")
                .build());

        log();

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active FROM users ", select()
                .columns("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .from("users")
                .build());

        log();

        assertEquals("SELECT name , CASE WHEN status = 'active' THEN 'Yes' ELSE 'No' END AS is_active FROM users ", select()
                .column("name")
                .caseStatement()
                .when("status = 'active'")
                .then("'Yes'")
                .elseCase("'No'")
                .endAs("is_active")
                .from("users")
                .build());

        log();

        assertEquals("SELECT name FROM customers WHERE age > 30 OR city = 'Los Angeles' ", select()
                .column("name")
                .from("customers")
                .where("age > 30")
                .or("city = 'Los Angeles'")
                .build());

        log();
    }

    private static void log() {
        Log.infof("Test %d passed.", ++passesTests);
    }
}