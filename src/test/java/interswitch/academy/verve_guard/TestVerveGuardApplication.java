package interswitch.academy.verve_guard;

import interswitch.academy.verve_guard.config.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

public class TestVerveGuardApplication {

    public static void main(String[] args) {
        SpringApplication.from(VerveGuardApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
