package core.project.chess.infrastructure.dal.util.sql;

import org.junit.jupiter.api.Test;

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.insert;
import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.withAndInsert;
import static core.project.chess.infrastructure.dal.util.sql.SelectBuilderTest.log;
import static org.junit.jupiter.api.Assertions.*;

class InsertBuilderTest {

    @Test
    void test() {
        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .build());

        log();

        assertEquals("INSERT INTO employees DEFAULT VALUES ",
                insert()
                        .defaultValues("employees")
                        .build());

        log();

        assertEquals("WITH temp AS (SELECT id, name FROM old_employees) INSERT INTO employees (id, name) SELECT id, name FROM temp ",
                withAndInsert("temp", "SELECT id, name FROM old_employees")
                        .into("employees", "id", "name")
                        .select()
                        .columns("id", "name")
                        .from("temp")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doNothing()
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary WHERE department_id = ? ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary WHERE department_id = ?")
                        .build());

        log();

        assertEquals("INSERT INTO products (category, price) SELECT category , SUM(price) FROM old_products GROUP BY category ",
                insert()
                        .into("products", "category", "price")
                        .select()
                        .column("category")
                        .sum("price")
                        .from("old_products")
                        .groupBy("category")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) RETURNING id, name ",
                insert()
                        .into("employees")
                        .columns("id", "name", "salary")
                        .values(3)
                        .returning("id", "name"));

        log();

        assertEquals("INSERT INTO orders (order_id, customer_id) VALUES (?, ?) RETURNING *",
                insert()
                        .into("orders", "order_id", "customer_id")
                        .values(2)
                        .returningAll());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doNothing()
                        .build());

        log();

        assertEquals("INSERT INTO inventory (item_id, quantity) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET quantity = quantity + EXCLUDED.quantity ",
                insert()
                        .into("inventory", "item_id", "quantity")
                        .values(2)
                        .onConflict("item_id")
                        .doUpdateSet("quantity = quantity + EXCLUDED.quantity")
                        .build());

        log();

        assertEquals("INSERT INTO tasks (task_id, status) VALUES (?, ?) ON CONFLICT (task_id) DO UPDATE SET status = ? WHERE status != ? ",
                insert()
                        .into("tasks", "task_id", "status")
                        .values(2)
                        .onConflict("task_id")
                        .doUpdateSet("status = ? WHERE status != ?")
                        .build());

        log();

    }

    @Test
    void test2() {
        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) RETURNING id ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .returning("id"));

        log();

        assertEquals("INSERT INTO employees (id, name) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name ",
                insert()
                        .into("employees", "id", "name")
                        .values(2)
                        .onConflict("id")
                        .doUpdateSet("name = EXCLUDED.name")
                        .build());

        log();

        assertEquals("INSERT INTO departments (id, department_name) DEFAULT VALUES ",
                insert()
                        .defaultValues("departments", "id", "department_name")
                        .build());

        log();

        assertEquals("INSERT INTO products (category, price) VALUES (?, ?) ON CONFLICT (category) DO NOTHING ",
                insert()
                        .into("products", "category", "price")
                        .values(2)
                        .onConflict("category")
                        .doNothing()
                        .build());

        log();

        assertEquals("WITH temp AS (SELECT id, name FROM old_employees) INSERT INTO employees (id, name) SELECT id, name FROM temp ",
                withAndInsert("temp", "SELECT id, name FROM old_employees")
                        .into("employees", "id", "name")
                        .select()
                        .columns("id", "name")
                        .from("temp")
                        .build());

        log();

        assertEquals("INSERT INTO orders (order_id, status) VALUES (?, ?) RETURNING *",
                insert()
                        .into("orders", "order_id", "status")
                        .values(2)
                        .returningAll());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET salary = EXCLUDED.salary WHERE department_id = ? ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doUpdateSet("salary = EXCLUDED.salary WHERE department_id = ?")
                        .build());

        log();

        assertEquals("INSERT INTO projects (project_id, project_name) SELECT project_id, project_name FROM old_projects ",
                insert()
                        .into("projects", "project_id", "project_name")
                        .select()
                        .columns("project_id", "project_name")
                        .from("old_projects")
                        .build());

        log();

        assertEquals("INSERT INTO users (username, email) VALUES (?, ?) ON CONFLICT (username) DO UPDATE SET email = EXCLUDED.email ",
                insert()
                        .into("users", "username", "email")
                        .values(2)
                        .onConflict("username")
                        .doUpdateSet("email = EXCLUDED.email")
                        .build());

        log();

        assertEquals("INSERT INTO inventory (item_id, quantity) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET quantity = quantity + EXCLUDED.quantity WHERE quantity < 100 ",
                insert()
                        .into("inventory", "item_id", "quantity")
                        .values(2)
                        .onConflict("item_id")
                        .doUpdateSet("quantity = quantity + EXCLUDED.quantity WHERE quantity < 100")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING RETURNING id ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doNothing()
                        .returning("id"));

        log();

        assertEquals("INSERT INTO books (book_id, title, author) VALUES (?, ?, ?) ON CONFLICT (book_id) DO NOTHING ",
                insert()
                        .into("books", "book_id", "title", "author")
                        .values(3)
                        .onConflict("book_id")
                        .doNothing()
                        .build());

        log();

        assertEquals("INSERT INTO inventory (item_id, price) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET price = EXCLUDED.price ",
                insert()
                        .into("inventory", "item_id", "price")
                        .values(2)
                        .onConflict("item_id")
                        .doUpdateSet("price = EXCLUDED.price")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name) VALUES (?, ?) RETURNING *",
                insert()
                        .into("employees", "id", "name")
                        .values(2)
                        .returningAll());

        log();

        assertEquals("INSERT INTO orders (order_id, customer_id, total) VALUES (?, ?, ?) ON CONFLICT (order_id) DO NOTHING ",
                insert()
                        .into("orders", "order_id", "customer_id", "total")
                        .values(3)
                        .onConflict("order_id")
                        .doNothing()
                        .build());

        log();

        assertEquals("INSERT INTO teams (team_id, team_name) VALUES (?, ?) ON CONFLICT (team_id) DO UPDATE SET team_name = EXCLUDED.team_name ",
                insert()
                        .into("teams", "team_id", "team_name")
                        .values(2)
                        .onConflict("team_id")
                        .doUpdateSet("team_name = EXCLUDED.team_name")
                        .build());

        log();

        assertEquals("INSERT INTO employees (id, name, salary) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name ",
                insert()
                        .into("employees", "id", "name", "salary")
                        .values(3)
                        .onConflict("id")
                        .doUpdateSet("name = EXCLUDED.name")
                        .build());

        log();

        assertEquals("INSERT INTO customers (customer_id, customer_name) VALUES (?, ?) ON CONFLICT (customer_id) DO UPDATE SET customer_name = EXCLUDED.customer_name ",
                insert()
                        .into("customers", "customer_id", "customer_name")
                        .values(2)
                        .onConflict("customer_id")
                        .doUpdateSet("customer_name = EXCLUDED.customer_name")
                        .build());

        log();

        assertEquals("INSERT INTO projects (project_id, project_name) VALUES (?, ?) RETURNING project_name ",
                insert()
                        .into("projects", "project_id", "project_name")
                        .values(2)
                        .returning("project_name"));

        log();

        assertEquals("INSERT INTO items (item_id, item_name) VALUES (?, ?) ON CONFLICT (item_id) DO UPDATE SET item_name = EXCLUDED.item_name ",
                insert()
                        .into("items", "item_id", "item_name")
                        .values(2)
                        .onConflict("item_id")
                        .doUpdateSet("item_name = EXCLUDED.item_name")
                        .build());

        log();
    }
}