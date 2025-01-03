package core.project.chess.infrastructure.dal.util.sql;

public class WhereDeleteBuilder {
    private final StringBuilder query;

    WhereDeleteBuilder(StringBuilder query) {
        this.query = query;
    }

    public WhereDeleteBuilder and(String condition) {
        query.append("AND ").append(condition).append(" ");
        return this;
    }

    public WhereDeleteBuilder or(String condition) {
        query.append("OR ").append(condition).append(" ");
        return this;
    }

    public String build() {
        return this.query.toString();
    }
}
