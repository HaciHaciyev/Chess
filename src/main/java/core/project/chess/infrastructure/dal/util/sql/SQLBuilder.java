package core.project.chess.infrastructure.dal.util.sql;

public class SQLBuilder {

    private SQLBuilder() {}

    public static SelectBuilder withAndSelect(String table, String subQuery) {
        return SelectBuilder.with(table, subQuery);
    }

    public static SelectBuilder select() {
        return SelectBuilder.select();
    }

    public static SelectBuilder selectDistinct() {
        return SelectBuilder.selectDistinct();
    }

    public static InsertBuilder insert() {
        return new InsertBuilder();
    }

    public static UpdateBuilder update() {
        return new UpdateBuilder();
    }

    public static DeleteBuilder delete() {
        return new DeleteBuilder();
    }
}