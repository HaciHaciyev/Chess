package core.project.chess.infrastructure.dal.util.sql;

public class FromBuilder {
    private final StringBuilder query;

    FromBuilder(StringBuilder query) {
        this.query = query.append("FROM ");
    }

    public JoinBuilder from(String table) {
        query.append(table).append(" ");
        return new JoinBuilder(query);
    }

    public JoinBuilder fromAs(String table, String alias) {
        query.append(table).append(" AS ").append(alias).append(" ");
        return new JoinBuilder(query);
    }

    public String limitAndOffset() {
        query.append("LIMIT ").append("? ").append("OFFSET ").append("? ");
        return this.query.toString();
    }

    public String limitAndOffset(int limit, int offset) {
        query.append("LIMIT ").append(limit).append(" ").append("OFFSET ").append(offset).append(" ");
        return this.query.toString();
    }

    public String build() {
        return this.query.toString();
    }
}
