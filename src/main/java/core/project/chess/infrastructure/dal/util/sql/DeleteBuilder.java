package core.project.chess.infrastructure.dal.util.sql;

public class DeleteBuilder {
    final StringBuilder qb;

    DeleteBuilder() {
        this.qb = new StringBuilder("DELETE ");
    }
}
