package core.project.chess.infrastructure.dal.util.sql;

import org.junit.jupiter.api.Test;

import static core.project.chess.infrastructure.dal.util.sql.SQLBuilder.select;
import static org.junit.jupiter.api.Assertions.*;

class SelectBuilderTest {

    @Test
    void test() {
        assertEquals("SELECT name, age FROM users WHERE age > 18 ORDER BY age DESC LIMIT 10 OFFSET 5 ", select()
                .columns("name", "age")
                .from("users")
                .where("age > 18")
                .orderBy("age", Order.DESC)
                .limitAndOffset(10, 5));

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


    }
}