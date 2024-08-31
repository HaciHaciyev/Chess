package core.project.chess.infrastructure.config.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Singleton
class DataSourceProvider {

    private final DataSource dataSource;

    private static final String DATA_SOURCE_URL = "jdbc:postgresql://localhost:5432/chessrepository";
    private static final String DATA_SOURCE_USERNAME = "root";
    private static final String DATA_SOURCE_PASSWORD = "749da8cc-e97c-4ea1-a79f-14567b88b4fc";

    DataSourceProvider() {
        var hikari = new HikariConfig();

        hikari.setJdbcUrl(DATA_SOURCE_URL);
        hikari.setUsername(DATA_SOURCE_USERNAME);
        hikari.setPassword(DATA_SOURCE_PASSWORD);

        hikari.setPoolName("Chessrepository-Pool");
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(2);
        hikari.setIdleTimeout(30000);
        hikari.setConnectionTimeout(30000);

        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        hikari.addDataSourceProperty("useLocalSessionState", "true");
        hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikari.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikari.addDataSourceProperty("cacheServerConfiguration", "true");
        hikari.addDataSourceProperty("elideSetAutoCommits", "true");
        hikari.addDataSourceProperty("maintainTimeStats", "false");

        this.dataSource = new HikariDataSource(hikari);
        migrate();
    }

    DataSource dataSource() {
        return dataSource;
    }

    @PostConstruct
    public void migrate() {
        try {
            var flyway = Flyway.configure()
                    .dataSource(DATA_SOURCE_URL, DATA_SOURCE_USERNAME, DATA_SOURCE_PASSWORD)
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}
