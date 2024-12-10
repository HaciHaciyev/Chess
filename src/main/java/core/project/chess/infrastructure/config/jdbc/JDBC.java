package core.project.chess.infrastructure.config.jdbc;

import core.project.chess.infrastructure.exceptions.persistant.DataNotFoundException;
import core.project.chess.infrastructure.exceptions.persistant.InvalidDataArgumentException;
import core.project.chess.infrastructure.exceptions.persistant.RepositoryDataException;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.repository.ResultSetExtractor;
import io.quarkus.logging.Log;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.function.Function;

/**
 * The {@code JDBC} class provides a set of utility methods for interacting with a relational database using JDBC (Java Database Connectivity).
 * It simplifies the execution of SQL queries and the handling of results, while also providing error handling and transaction management.
 * This class is designed to be used as a singleton within a DI container.
 *
 * <h2>Class API Overview</h2>
 * The {@code JDBC} class offers the following key functionalities:
 * <ul>
 *     <li><strong>Read Operations:</strong> Methods to execute SQL queries that return single values, objects, or lists of objects.</li>
 *     <li><strong>Write Operations:</strong> Methods to execute SQL updates, including single updates, updates with array parameters, and batch updates.</li>
 *     <li><strong>Error Handling:</strong> Built-in mechanisms to handle SQL exceptions and translate them into application-specific exceptions.</li>
 *     <li><strong>Transaction Management:</strong> Automatic management of transactions for write operations, ensuring data integrity.</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 * To use the {@code JDBC} class effectively, follow these guidelines:
 * <ol>
 *     <li><strong>Initialization:</strong> Ensure that the {@code JDBC} instance is properly initialized with a valid {@code DataSource} before use.</li>
 *     <li><strong>Read Operations:</strong> Use the {@code read()} method to fetch single values or objects. For complex mappings, consider using the {@code read()} method with a {@code ResultSetExtractor} or {@code RowMapper}.</li>
 *     <li><strong>Write Operations:</strong> Use the {@code write()} method for standard updates. For updates involving arrays, use {@code writeArrayOf()}. For bulk operations, utilize {@code writeBatch()}.</li>
 *     <li><strong>Parameter Handling:</strong> Always ensure that parameters passed to SQL statements are properly sanitized and validated to prevent SQL injection attacks.</li>
 *     <li><strong>Error Handling:</strong> Check the result of each operation. Use the {@code Result} object to determine success or failure and handle errors appropriately.</li>
 *     <li><strong>Connection Management:</strong> The class manages connections internally, so there is no need to manually open or close connections. However, ensure that the {@code DataSource} is properly configured.</li>
 * </ol>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * // Initialize the JDBC instance with a DataSource
 * DataSource dataSource = ...; // Obtain a DataSource instance
 * JDBC jdbc = new JDBC(dataSource);
 *
 * // Insert a new product
 * String insertSQL = "INSERT INTO products (name, price) VALUES (?, ?)";
 * Result<Boolean, Throwable> insertResult = jdbc.write(insertSQL, "New Product", 29.99);
 *
 * // Fetch a product by ID
 * String selectSQL = "SELECT * FROM products WHERE id = ?";
 * Result<Product, Throwable> productResult = jdbc.read(selectSQL, Product.class, 1);
 *
 * // Update product tags
 * String updateTagsSQL = "UPDATE products SET tags = ? WHERE id = ?";
 * String[] tags = {"electronics", "sale"};
 * Result<Boolean, Throwable> updateResult = jdbc.writeArrayOf(updateTagsSQL, "text", 1, tags, 1);
 *
 * // Batch insert customers
 * String batchInsertSQL = "INSERT INTO customers (name, email) VALUES (?, ?)";
 * List<Object[]>batchArgs = Arrays.asList(
 *     new Object[]{"Alice", "alice@example.com"},
 *     new Object[]{"Bob", "bob@example.com"}
 * );
 * Result<Boolean, Throwable> batchResult = jdbc.writeBatch(batchInsertSQL, batchArgs);
 * </pre>
 *
 * @author Hadzhyiev Hadzhy
 * @version 1.0
 */
@ApplicationScoped
public class JDBC {

    private final DataSource dataSource;

    private static final String SQL_STATUS_NO_DATA = "02000";

    private static final Map<Class<?>, Function<ResultSet, ?>> wrapperMapFunctions = getWrapperMap();

    private static final Set<Class<?>> wrapperTypes = Set.of(
            Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class
    );

    JDBC(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Executes a SQL query and uses a {@code ResultSetExtractor} to map the result set to an object.
     *
     * @param sql      the SQL query to execute
     * @param extractor a functional interface for extracting data from the {@code ResultSet}
     * @param params   optional parameters for the SQL query
     * @param <T>     the type of the extracted object
     * @return a {@code Result<T, Throwable>} containing the extracted object or an error
     * @throws NullPointerException if {@code sql} or {@code extractor} is {@code null}
     *
     * @example
     * <pre>
     * Result<UserAccount, Throwable> userResult = jdbc.read(
     *          FIND_BY_ID,
     *          this::userAccountMapper,
     *          userId.toString()
     * );
     * </pre>
     */
    public <T> Result<T, Throwable> read(final String sql, final ResultSetExtractor<T> extractor, @Nullable final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(extractor);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
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
            Log.error(e);
            return handleSQLException(e);
        }
    }

    /**
     * Executes a SQL query that returns a single value of a specified wrapper type.
     *
     * @param sql  the SQL query to execute
     * @param type the expected return type, which must be a wrapper type (e.g., Integer.class)
     * @param params optional parameters for the SQL query
     * @param <T>  the type of the result
     * @return a {@code Result<T, Throwable>} containing the result or an error
     * @throws NullPointerException if {@code sql} or {@code type} is {@code null}
     * @throws InvalidDataArgumentException if the specified type is not a valid wrapper type
     *
     * @example
     * <pre>
     * Integer count = jdbc.readObjectOf(
     *          "SELECT COUNT(email) FROM YOU_TABLE WHERE email = ?",
     *          Integer.class,
     *          verifiableEmail.email()
     * )
     *          .orElseThrow();
     * </pre>
     */
    public <T> Result<T, Throwable> readObjectOf(final String sql, final Class<T> type, @Nullable final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(type);

        final boolean isWrapper = wrapperTypes.contains(type);
        if (!isWrapper && type != String.class || type == void.class) {
            return Result.failure(
                    new InvalidDataArgumentException("Invalid class type. Function jdbc.queryForObjets() can only provide primitive wrappers and String.")
            );
        }

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            if (params != null && params.length > 0) {
                setParameters(statement, params);
            }

            try (final ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Result.failure(new DataNotFoundException("Data in query for object was not found."));
                }

                return Result.success((T) wrapperMapFunctions.get(type).apply(resultSet));
            }
        } catch (SQLException | IllegalArgumentException e) {
            Log.error(e);
            if (e instanceof IllegalArgumentException) {
                return Result.failure(e);
            }

            return handleSQLException((SQLException) e);
        }
    }

    /**
     * Executes a SQL query that returns a list of objects mapped by a {@code RowMapper}.
     *
     * @param sql      the SQL query to execute
     * @param extractor a functional interface for mapping rows of the result set to objects
     * @param <T>     the type of the mapped objects
     * @return a {@code Result<List<T>, Throwable>} containing the list of mapped objects or an error
     * @throws NullPointerException if {@code sql} or {@code rowMapper} is {@code null}
     *
     * @example
     * <pre>
     * Result<List<UserAccount>, Throwable> users = jdbc.readListOf(
     *          "SELECT * FROM UserAccount",
     *          this::userAccountMapper
     * );
     * </pre>
     */
    public <T> Result<List<T>, Throwable> readListOf(final String sql, final ResultSetExtractor<T> extractor, @Nullable final Object... params) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(extractor);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            if (params != null && params.length > 0) {
                setParameters(statement, params);
            }

            final List<T> results = new ArrayList<>();
            try (final ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    T item = extractor.extractData(resultSet);
                    results.add(item);
                }
            }

            return Result.success(results);
        } catch (SQLException e) {
            Log.error(e);
            return handleSQLException(e);
        }
    }

    /**
     * Executes a SQL update (INSERT, UPDATE, DELETE) and manages transactions.
     *
     * @param sql   the SQL update statement to execute
     * @param args  parameters for the SQL statement
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     * @throws NullPointerException if {@code sql} or {@code args} is {@code null}
     *
     * @example
     * <pre>
     * String insertProductSQL = "INSERT INTO products (name, price) VALUES (?, ?)";
     * String productName = "Sample Product";
     * double productPrice = 19.99;
     *
     * Result<Boolean, Throwable> result = jdbc.write(insertProductSQL, productName, productPrice);
     * if (result.isSuccess()) {
     *     System.out.println("Product inserted successfully.");
     * } else {
     *     System.err.println("Failed to insert product: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> write(final String sql, final Object... args) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(args);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            statement.executeUpdate();

            return Result.success(Boolean.TRUE);
        } catch (SQLException e) {
            Log.error(e);
            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    /**
     * Executes a SQL update that includes an array parameter.
     *
     * @param sql             the SQL update statement to execute
     * @param arrayDefinition the SQL type of the array
     * @param arrayIndex      the index of the array parameter in the SQL statement
     * @param array           the array to be passed to the SQL statement
     * @param args            additional parameters for the SQL statement
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     * @throws NullPointerException if {@code sql} or {@code args} is {@code null}
     *
     * @example
     * <pre>
     * String updateProductTagsSQL = "UPDATE products SET tags = ? WHERE id = ?";
     *
     * String arrayDefinition = "text"; // Assuming the database supports a text array
     * int arrayIndex = 1; // The index of the array parameter in the SQL statement
     * String[] tags = {"electronics", "gadget"};
     * int productId = 1; // The ID of the product to update
     *
     * Result<Boolean, Throwable> result = jdbc.writeArrayOf(
     *          updateProductTagsSQL,
     *          arrayDefinition,
     *          arrayIndex,
     *          tags,
     *          productId
     * );
     *
     * if (result.isSuccess()) {
     *     System.out.println("Product tags updated successfully.");
     * } else {
     *     System.err.println("Failed to update product tags: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> writeArrayOf(final String sql, final String arrayDefinition, final byte arrayIndex,
                                                   final Object[] array, final Object... args) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(args);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {
            setParameters(statement, args);
            final Array createdArray = connection.createArrayOf(arrayDefinition, array);
            statement.setArray(arrayIndex, createdArray);
            statement.executeUpdate();

            return Result.success(true);
        } catch (SQLException e) {
            Log.error(e);
            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    /**
     * Executes a batch of SQL updates.
     *
     * @param sql       the SQL update statement to execute
     * @param batchArgs a list of parameter arrays for the batch execution
     * @return a {@code Result<Boolean, Throwable>} indicating success or failure
     * @throws NullPointerException if {@code sql} or {@code batchArgs} is {@code null}
     *
     * @example
     * <pre>
     * String insertCustomerSQL = "INSERT INTO customers (name, email) VALUES (?, ?)";
     * List<Object[]> batchArgs = Arrays.asList(
     *     new Object[]{"Alice", "alice@example.com"},
     *     new Object[]{"Bob", "bob@example.com"},
     *     new Object[]{"Charlie", "charlie@example.com"}
     * );
     *
     * Result<Boolean, Throwable> result = jdbc.writeBatch(insertCustomerSQL, batchArgs);
     * if (result.isSuccess()) {
     *     System.out.println("Customers inserted successfully.");
     * } else {
     *     System.err.println("Failed to insert customers: " + result.getError().getMessage());
     * }
     * </pre>
     */
    public Result<Boolean, Throwable> writeBatch(final String sql, final List<Object[]> batchArgs) {
        Objects.requireNonNull(sql);
        Objects.requireNonNull(batchArgs);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(sql)) {

            for (Object[] args : batchArgs) {
                setParameters(statement, args);
                statement.addBatch();
            }

            statement.executeBatch();

            return Result.success(true);
        } catch (SQLException e) {
            Log.error(e);
            return Result.failure(new RepositoryDataException(e.getMessage()));
        }
    }

    /**
     * Sets the parameters for a {@code PreparedStatement}.
     *
     * @param statement the {@code PreparedStatement} to set parameters for
     * @param params    the parameters to set
     * @throws SQLException if an SQL error occurs while setting parameters
     */
    private void setParameters(final PreparedStatement statement, final Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }

    /**
     * Returns a map of wrapper types to their corresponding functions for extracting values from a {@code ResultSet}.
     *
     * @return a map of wrapper types to functions
     */
    private static Map<Class<?>, Function<ResultSet, ?>> getWrapperMap() {
        return Map.of(
                String.class, rs -> {
                    try {
                        return rs.getString(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to String.");
                    }
                },
                Boolean.class, rs -> {
                    try {
                        return rs.getBoolean(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Boolean.");
                    }
                },
                Character.class, rs -> {
                    try {
                        return rs.getString(1).charAt(0);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Character.");
                    }
                },
                Byte.class, rs -> {
                    try {
                        return rs.getByte(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Byte.");
                    }
                },
                Short.class, rs -> {
                    try {
                        return rs.getShort(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Short.");
                    }
                },
                Integer.class, rs -> {
                    try {
                        return rs.getInt(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Integer.");
                    }
                },
                Long.class, rs -> {
                    try {
                        return rs.getLong(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Long.");
                    }
                },
                Float.class, rs -> {
                    try {
                        return rs.getFloat(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Float.");
                    }
                },
                Double.class, rs -> {
                    try {
                        return rs.getDouble(1);
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Invalid object type. Can`t cast to Double.");
                    }
                }
        );
    }

    /**
     * Handles SQL exceptions and translates them into application-specific exceptions.
     *
     * @param e the {@code SQLException} to handle
     * @param <T> the type of the result
     * @return a {@code Result<T, Throwable>} containing the appropriate error
     */
    private <T> Result<T, Throwable> handleSQLException(final SQLException e) {
        final String sqlStatus = e.getSQLState();

        if (sqlStatus.equals(SQL_STATUS_NO_DATA)) {
            return Result.failure(new DataNotFoundException("Data was not found."));
        }

        return Result.failure(new RepositoryDataException(e.getMessage()));
    }
}
