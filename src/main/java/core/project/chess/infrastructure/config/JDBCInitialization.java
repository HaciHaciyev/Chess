package core.project.chess.infrastructure.config;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import javax.sql.DataSource;

@Startup
@ApplicationScoped
public class JDBCInitialization {

    private final DataSource dataSource;

    public JDBCInitialization(Instance<DataSource> dataSource) {
        this.dataSource = dataSource.get();
    }

    @PostConstruct
    public void init() {
        JetQuerious.init(dataSource);
    }
}
