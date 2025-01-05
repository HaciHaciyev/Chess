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

    @Test
    void test2() {
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

        assertEquals("SELECT id, name FROM employees WHERE department_id IN (1, 2, 3) AND salary > 50000 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id IN (1, 2, 3)")
                .and("salary > 50000")
                .build());

        log();


        assertEquals("SELECT * FROM orders WHERE status = 'pending' LIMIT 10 OFFSET 20 ", select()
                .all()
                .from("orders")
                .where("status = 'pending'")
                .limitAndOffset(10, 20));

        log();

        assertEquals("SELECT category , AVG(price) AS avg_price FROM products GROUP BY category ", select()
                .columns("category")
                .avg("price")
                .as("avg_price")
                .from("products")
                .groupBy("category")
                .build());

        log();

        assertEquals("SELECT category , COUNT(*) AS product_count FROM products GROUP BY category HAVING COUNT(*) > 10 ", select()
                .columns("category")
                .count("*")
                .as("product_count")
                .from("products")
                .groupBy("category")
                .having("COUNT(*) > 10")
                .build());

        log();

        assertEquals("SELECT p.id, p.name FROM products p INNER JOIN categories c ON p.category_id = c.id WHERE c.name = 'Electronics' ", select()
                .columns("p.id", "p.name")
                .from("products p")
                .innerJoin("categories c", "p.category_id = c.id")
                .where("c.name = 'Electronics'")
                .build());

        log();

        assertEquals("SELECT id, name FROM employees WHERE (status = 'active' AND age > 30) OR department = 'HR' ", select()
                .columns("id", "name")
                .from("employees")
                .where("(status = 'active' AND age > 30)")
                .or("department = 'HR'")
                .build());

        log();

        assertEquals("SELECT id, name FROM users WHERE NOT (age < 18) ", select()
                .columns("id", "name")
                .from("users")
                .whereNot("(age < 18)")
                .build());

        log();

        assertEquals("WITH active_users AS (SELECT id FROM users WHERE status = 'active') SELECT * FROM active_users ",
                withAndSelect("active_users", "SELECT id FROM users WHERE status = 'active'")
                .columns("*")
                .from("active_users")
                .build());

        log();

        assertEquals("SELECT name FROM products WHERE price BETWEEN 50 AND 100 ", select()
                .column("name")
                .from("products")
                .where("price BETWEEN 50 AND 100")
                .build());

        log();

    }

    @Test
    void test3() {
        assertEquals("SELECT id, name FROM employees WHERE department_id = 3 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id = 3")
                .build());

        log();

        assertEquals("SELECT name, age FROM users WHERE age >= 25 AND age <= 40 ", select()
                .columns("name", "age")
                .from("users")
                .where("age >= 25")
                .and("age <= 40")
                .build());

        log();

        assertEquals("SELECT DISTINCT city FROM customers ", selectDistinct()
                .column("city")
                .from("customers")
                .build());

        log();

        assertEquals("SELECT id, email FROM users WHERE email LIKE '%@example.com' ", select()
                .columns("id", "email")
                .from("users")
                .where("email LIKE '%@example.com'")
                .build());

        log();

        assertEquals("SELECT id, name FROM products WHERE name IS NOT NULL ", select()
                .columns("id", "name")
                .from("products")
                .where("name IS NOT NULL")
                .build());

        log();

        assertEquals("SELECT category , SUM(price) AS total_price FROM products GROUP BY category ORDER BY total_price DESC ", select()
                .columns("category")
                .sum("price")
                .as("total_price")
                .from("products")
                .groupBy("category")
                .orderBy("total_price", Order.DESC)
                .build());

        log();

        assertEquals("SELECT name FROM users WHERE registration_date >= '2025-01-01' AND status = 'active' ", select()
                .column("name")
                .from("users")
                .where("registration_date >= '2025-01-01'")
                .and("status = 'active'")
                .build());

        log();

        assertEquals("SELECT department_id , COUNT(*) AS employee_count FROM employees GROUP BY department_id HAVING COUNT(*) > 5 ", select()
                .columns("department_id")
                .count("*")
                .as("employee_count")
                .from("employees")
                .groupBy("department_id")
                .having("COUNT(*) > 5")
                .build());

        log();

        assertEquals("SELECT id, name , CASE WHEN salary > 50000 THEN 'high' ELSE 'low' END AS salary_level FROM employees ", select()
                .columns("id", "name")
                .caseStatement()
                .when("salary > 50000")
                .then("'high'")
                .elseCase("'low'")
                .endAs("salary_level")
                .from("employees")
                .build());

        log();

        assertEquals("SELECT id, name FROM users WHERE last_login IS NULL ", select()
                .columns("id", "name")
                .from("users")
                .where("last_login IS NULL")
                .build());

        log();

        assertEquals("SELECT country , AVG(age) AS avg_age FROM users GROUP BY country ", select()
                .columns("country")
                .avg("age")
                .as("avg_age")
                .from("users")
                .groupBy("country")
                .build());

        log();

        assertEquals("SELECT name , COUNT(*) AS order_count FROM customers c INNER JOIN orders o ON c.id = o.customer_id GROUP BY c.name ", select()
                .columns("name")
                .count("*")
                .as("order_count")
                .from("customers c")
                .innerJoin("orders o", "c.id = o.customer_id")
                .groupBy("c.name")
                .build());

        log();

        assertEquals("SELECT name , MAX(salary) AS max_salary FROM employees GROUP BY department_id ", select()
                .columns("name")
                .max("salary")
                .as("max_salary")
                .from("employees")
                .groupBy("department_id")
                .build());

        log();

        assertEquals("SELECT id, name FROM employees WHERE department_id = 5 OR salary > 60000 ", select()
                .columns("id", "name")
                .from("employees")
                .where("department_id = 5")
                .or("salary > 60000")
                .build());

        log();

        assertEquals("SELECT id, salary FROM employees WHERE NOT (department_id = 1 AND salary < 40000) ", select()
                .columns("id", "salary")
                .from("employees")
                .whereNot("(department_id = 1 AND salary < 40000)")
                .build());

        log();

        assertEquals("SELECT id, name FROM users WHERE age BETWEEN 20 AND 30 ", select()
                .columns("id", "name")
                .from("users")
                .where("age BETWEEN 20 AND 30")
                .build());

        log();

        assertEquals("SELECT * FROM orders WHERE created_at >= '2025-01-01' ORDER BY created_at ASC LIMIT 10 OFFSET 0 ", select()
                .all()
                .from("orders")
                .where("created_at >= '2025-01-01'")
                .orderBy("created_at", Order.ASC)
                .limitAndOffset(10, 0));

        log();

        assertEquals("SELECT category , SUM(price) AS total_price FROM products WHERE category IS NOT NULL GROUP BY category HAVING SUM(price) > 1000 ", select()
                .columns("category")
                .sum("price")
                .as("total_price")
                .from("products")
                .where("category IS NOT NULL")
                .groupBy("category")
                .having("SUM(price) > 1000")
                .build());

        log();
    }

    private static void log() {
        Log.infof("Test %d passed.", ++passesTests);
    }
}