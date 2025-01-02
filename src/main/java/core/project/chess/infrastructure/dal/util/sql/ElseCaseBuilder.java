package core.project.chess.infrastructure.dal.util.sql;

public class ElseCaseBuilder {
    private final StringBuilder query;

    public ElseCaseBuilder(StringBuilder query) {
        this.query = query;
    }

    public FromBuilder end() {
        query.append("END ");
        return new FromBuilder(query);
    }

    public FromBuilder endAs(String alias) {
        query.append("END AS ").append(alias).append(" ");
        return new FromBuilder(query);
    }
}
