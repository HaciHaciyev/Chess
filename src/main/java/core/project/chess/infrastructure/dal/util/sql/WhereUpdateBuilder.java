package core.project.chess.infrastructure.dal.util.sql;

public class WhereUpdateBuilder {
    private final StringBuilder query;

    WhereUpdateBuilder(StringBuilder query) {
        this.query = query;
    }

    public WhereUpdateBuilder and(String condition) {
        query.append("AND ").append(condition).append(" ");
        return this;
    }

    public WhereUpdateBuilder or(String condition) {
        query.append("OR ").append(condition).append(" ");
        return this;
    }

    public String returning(String... columns) {
        return this.query.append("RETURNING ").append(String.join(", ", columns)).append(" ").toString();
    }

    public String build() {
        return this.query.toString();
    }
}
