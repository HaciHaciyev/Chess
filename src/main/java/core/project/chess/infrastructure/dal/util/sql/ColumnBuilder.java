package core.project.chess.infrastructure.dal.util.sql;

import static core.project.chess.infrastructure.dal.util.sql.Util.deleteSurplusComa;

public class ColumnBuilder {
    private final StringBuilder query;

    public ColumnBuilder(StringBuilder query) {
        this.query = query;
    }

    public ColumnBuilder column(String column) {
        query.append(column).append(" ");
        return new ColumnBuilder(query);
    }

    public ColumnBuilder as(String column) {
        query.append("AS ").append(column).append(" ,");
        return new ColumnBuilder(query);
    }

    public JoinBuilder from(String table) {
        deleteSurplusComa(query);

        query.append(table).append(" ");
        return new JoinBuilder(query);
    }
}
