package core.project.chess.infrastructure.config.jdbc;

import core.project.chess.infrastructure.exceptions.DataNotFoundException;
import core.project.chess.infrastructure.exceptions.RepositoryDataException;
import core.project.chess.infrastructure.utilities.OptionalArgument;
import core.project.chess.infrastructure.utilities.Result;
import core.project.chess.infrastructure.utilities.ResultSetExtractor;
import core.project.chess.infrastructure.utilities.RowMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class JDBC {

    private final DataSourceProvider dataSourceProvider;

    public static final String SQL_QUERY_LOGGING_FORMAT = "Executing sql query : {%s}";

    JDBC(DataSourceProvider dataSourceProvider) {
        this.dataSourceProvider = dataSourceProvider;
    }

    public <T> Result<T, Throwable> query(final String sql, final ResultSetExtractor<T> extractor, @OptionalArgument final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(extractor);

        Log.debug(SQL_QUERY_LOGGING_FORMAT.formatted(sql));

        try (
                final Connection connection = dataSourceProvider.dataSource().getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            setParameters(statement, params);

            try (final ResultSet resultSet = statement.executeQuery()) {

                resultSet.first();
                return Result.success(extractor.extractData(resultSet));

            }

        } catch (SQLException e) {

            final String sqlStatus = e.getSQLState();

            if (sqlStatus.equals("02000")) {
                return Result.failure(new DataNotFoundException("Data was not found."));
            }

            return Result.failure(new RepositoryDataException(e.getMessage()));

        }

    }

    public <T> Result<List<T>, Throwable> queryForList(final String sql, final RowMapper<T> rowMapper) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(rowMapper);

        Log.debug(SQL_QUERY_LOGGING_FORMAT.formatted(sql));

        try (
                final Connection connection = dataSourceProvider.dataSource().getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                final ResultSet resultSet = statement.executeQuery()
        ) {

            List<T> results = new ArrayList<>();

            int rowNum = 0;
            while (resultSet.next()) {
                results.add(rowMapper.extractData(resultSet, rowNum));
                rowNum++;
            }

            return Result.success(results);

        } catch (SQLException e) {

            final String sqlStatus = e.getSQLState();

            if (sqlStatus.equals("02000")) {
                return Result.failure(new DataNotFoundException("Data was not found."));
            }

            return Result.failure(new RepositoryDataException(e.getMessage()));

        }
    }

    public Result<Boolean, Throwable> update(final String sql, final Object... args) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(args);

        Log.debug(SQL_QUERY_LOGGING_FORMAT.formatted(sql));

        try (
                final Connection connection = dataSourceProvider.dataSource().getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            setParameters(statement, args);

            statement.executeUpdate();

            return Result.success(true);

        } catch (SQLException e) {

            return Result.failure(new RepositoryDataException(e.getMessage()));

        }
    }


    private void setParameters(final PreparedStatement statement, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
