package core.project.chess.infrastructure.config.jdbc;

import core.project.chess.infrastructure.exceptions.persistant.DataNotFoundException;
import core.project.chess.infrastructure.exceptions.persistant.InvalidDataArgumentException;
import core.project.chess.infrastructure.exceptions.persistant.RepositoryDataException;
import core.project.chess.infrastructure.utilities.OptionalArgument;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.repository.ResultSetExtractor;
import core.project.chess.infrastructure.utilities.repository.RowMapper;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Function;

@ApplicationScoped
public class JDBC {

    private final DataSource dataSource;

    private static final String SQL_STATUS_NO_DATA = "02000";

    public static final String SQL_QUERY_LOGGING_FORMAT = "Executing sql query : {%s}";

    private static final Map<Class<?>, Function<ResultSet, ?>> wrapperMapFunctions = getWrapperMap();

    private static final Set<Class<?>> wrapperTypes = Set.of(
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
    );

    JDBC(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> Result<T, Throwable> read(final String sql, final Class<T> type, @OptionalArgument final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(type);

        final boolean isWrapper = wrapperTypes.contains(type);
        if (!isWrapper || type == void.class) {
            return Result.failure(new InvalidDataArgumentException("Invalid class type. Function jdbc.queryForObjets() can only provide primitive wrappers."));
        }

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            connection.setReadOnly(true);
            if (params != null && params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new DataNotFoundException("Data in query for object was not found."));
                }

                return Result.success((T) wrapperMapFunctions.get(type).apply(resultSet));
            }

        } catch (SQLException e) {
            final String sqlStatus = e.getSQLState();

            if (sqlStatus.equals(SQL_STATUS_NO_DATA)) {
                return Result.failure(new DataNotFoundException("Data in query for object was not found."));
            }

            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor, @OptionalArgument final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(extractor);

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            connection.setReadOnly(true);
            if (params != null && params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new DataNotFoundException("Data in for this query was not found."));
                }

                return Result.success(extractor.extractData(resultSet));
            }

        } catch (SQLException e) {
            final String sqlStatus = e.getSQLState();

            if (sqlStatus.equals(SQL_STATUS_NO_DATA)) {
                return Result.failure(new DataNotFoundException("Data was not found."));
            }

            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    public <T> Result<List<T>, Throwable> readListOf(final String sql, final RowMapper<T> rowMapper) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(rowMapper);

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql);
                final ResultSet resultSet = statement.executeQuery()
        ) {
            final List<T> results = new ArrayList<>();

            int rowNum = 0;
            while (resultSet.next()) {
                results.add(rowMapper.extractData(resultSet, rowNum));
                rowNum++;
            }

            return Result.success(results);

        } catch (SQLException e) {
            final String sqlStatus = e.getSQLState();

            if (sqlStatus.equals(SQL_STATUS_NO_DATA)) {
                return Result.failure(new DataNotFoundException("Data was not found."));
            }

            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    public Result<Boolean, Throwable> write(final String sql, final Object... args) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(args);

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            connection.setAutoCommit(false);

            try {
                setParameters(statement, args);
                statement.executeUpdate();
                connection.commit();

                return Result.success(true);
            } catch (SQLException e) {
                connection.rollback();
                return Result.failure(new RepositoryDataException(e.getMessage()));
            }
        } catch (SQLException e) {
            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    public Result<Boolean, Throwable> writeArrayOf(final String sql, final String arrayDefinition, final byte arrayIndex,
                                                   final Object[] array, final Object... args) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(args);

        try (
                final Connection connection = dataSource.getConnection();
                final PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            connection.setAutoCommit(false);

            try {
                setParameters(statement, args);
                final Array createdArray = connection.createArrayOf(arrayDefinition, array);
                statement.setArray(arrayIndex, createdArray);
                statement.executeUpdate();
                connection.commit();

                return Result.success(true);
            } catch (SQLException e) {
                connection.rollback();
                return Result.failure(new RepositoryDataException(e.getMessage()));
            }
        } catch (SQLException e) {
            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    private void setParameters(final PreparedStatement statement, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    private static Map<Class<?>, Function<ResultSet, ?>> getWrapperMap() {
        return Map.of(
                Boolean.class, rs -> {
                    try {
                        return rs.getBoolean(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Character.class, rs -> {
                    try {
                        return rs.getString(1).charAt(0);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Byte.class, rs -> {
                    try {
                        return rs.getByte(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Short.class, rs -> {
                    try {
                        return rs.getShort(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Integer.class, rs -> {
                    try {
                        return rs.getInt(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Long.class, rs -> {
                    try {
                        return rs.getLong(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Float.class, rs -> {
                    try {
                        return rs.getFloat(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                },
                Double.class, rs -> {
                    try {
                        return rs.getDouble(1);
                    } catch (SQLException e) {
                        throw new RepositoryDataException();
                    }
                }
        );
    }
}
