package core.project.chess.infrastructure.config.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.quarkus.logging.Log;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

@Singleton
class DataSourceProvider {

    private final DataSource dataSource;

    @ConfigProperty(name = "data.source.url")
    String dataSourceUrl;

    @ConfigProperty(name = "data.source.username")
    String dataSourceUsername;

    @ConfigProperty(name = "data.source.password")
    String dataSourcePassword;

    DataSourceProvider() {
        var hikari = new HikariConfig();

        hikari.setJdbcUrl(dataSourceUrl);
        hikari.setUsername(dataSourceUsername);
        hikari.setPassword(dataSourcePassword);

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
                    .dataSource(dataSourceUrl, dataSourceUsername, dataSourcePassword)
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}
