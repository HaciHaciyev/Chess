package core.project.chess.infrastructure.utilities.repository;

import core.project.chess.infrastructure.exceptions.persistant.RepositoryDataException;
import jakarta.annotation.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {
    @Nullable
    T extractData(ResultSet rs, int rowNum) throws SQLException, RepositoryDataException;
}
