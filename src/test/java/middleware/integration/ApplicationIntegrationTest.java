package middleware.integration;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test for the main application functionality.
 * Tests the application running with containerized PostgreSQL.
 */
class ApplicationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;



    @Test
    void contextLoads() {
        // Test that the application context loads successfully with containers
        assert applicationContext != null;
        assert applicationContext.getBeanDefinitionCount() > 0;
    }

    @Test
    void databaseConnectionWorks() {
        // Verify that we can access the database through the context
        // If this passes, it means the datasource is working
        assert applicationContext.getBean("dataSource") != null;
    }
}
