package runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Runs ONLY the API feature, with its own glue package, so the browser hooks
 * and UI step definitions of the Selenium project are not loaded.
 *
 *   mvn test "-Dtest=ApiRunner"
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/apifeatures",
        glue = {"apistepdefinition"},
        plugin = {"pretty", "html:target/api-report.html"}
)
public class ApiRunner {
}
