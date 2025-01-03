package core.project.chess.infrastructure.dal.util.sql;

public class ChainedUpdateBuilder {
    private final StringBuilder query;

    ChainedUpdateBuilder(StringBuilder query) {
        this.query = query;
    }

    public WhereUpdateBuilder where(String condition) {
        query.append("WHERE ").append(condition).append(" ");
        return new WhereUpdateBuilder(query);
    }

    public WhereUpdateBuilder whereNot(String condition) {
        query.append("WHERE NOT ").append(condition).append(" ");
        return new WhereUpdateBuilder(query);
    }

    public String build() {
        return this.query.toString();
    }
}
