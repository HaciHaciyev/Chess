package core.project.chess.infrastructure.dal.util.sql;

public class InsertBuilder {
    final StringBuilder qb;

    InsertBuilder() {
        this.qb = new StringBuilder("INSERT ");
    }
}
