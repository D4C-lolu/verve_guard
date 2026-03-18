package interswitch.academy.verve_guard.base;

import interswitch.academy.verve_guard.config.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

    protected String uniqueIp() {
        int counter = (int) (System.nanoTime() % 900000) + 100000;
        return "10.0." + (counter / 1000) + "." + (counter % 255);
    }

    @Autowired
    private FlywayProperties flywayProperties;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetDatabase() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(flywayProperties.getLocations().toArray(new String[0]))
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
    }
}
