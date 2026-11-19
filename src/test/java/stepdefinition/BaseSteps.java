package stepdefinition;

import java.awt.GraphicsEnvironment;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import utils.ConfigReader;
import utils.WaitUtils;

// Driver setup + hooks. Pages get the driver from here.
public class BaseSteps {

	public static WebDriver driver;

	// Screen width (CSS pixels), pixel density and browser identity of each phone we can pretend to be
	private record Device(int width, double scale, String userAgent) {
	}

	private static final String ANDROID_USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
	private static final String IPHONE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) "
			+ "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";

	// the API scenarios (@api) need no browser
	@Before("not @api")
	public void startBrowser(Scenario scenario) {
		ChromeOptions options = new ChromeOptions();

		// hides the 'Chrome is being controlled by automated test software' bar
		options.setExperimentalOption("excludeSwitches", List.of("enable-automation"));
		if (Boolean.parseBoolean(ConfigReader.get("headless"))) {
			options.addArguments("--headless=new");
		}
		options.addArguments("--disable-notifications");

		// no 'Save password?' popup: turn off Chrome's password manager and use a private window,
		// which never offers to save passwords (newer Chrome versions can ignore the settings alone)
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("credentials_enable_service", false);
		prefs.put("profile.password_manager_enabled", false);
		prefs.put("profile.password_manager_leak_detection", false);
		// no 'wants to know your location' / notification pop-ups from the site (2 = block)
		prefs.put("profile.default_content_setting_values.geolocation", 2);
		prefs.put("profile.default_content_setting_values.notifications", 2);
		options.setExperimentalOption("prefs", prefs);
		options.addArguments("--incognito");

		driver = new ChromeDriver(options);
		Device device = deviceFor(ConfigReader.get("device"));
		int visibleHeight = sizeWindow(device);
		emulateMobile(device, visibleHeight);
		driver.get(ConfigReader.get("baseUrl"));

		// shows in the report so you know which device each result belongs to
		scenario.log("Device: " + ConfigReader.get("device"));
	}

	// Short pause after every step so each action can finish before the next one starts (and you can follow along)
	@AfterStep("not @api")
	public void pauseBetweenSteps() {
		WaitUtils.pause(Long.parseLong(ConfigReader.get("stepDelaySeconds")));
		if (driver != null) {
			new WaitUtils(driver).closePromoPopupIfShown();
		}
	}

	@After("not @api")
	public void closeBrowser(Scenario scenario) {
		if (scenario.isFailed() && driver != null) {
			byte[] shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
			scenario.attach(shot, "image/png", "Failure screenshot");
		}
		if (driver != null) {
			driver.quit();
		}
	}

	// device comes from config.properties or -Ddevice="iPhone 12 Pro"
	private Device deviceFor(String name) {
		String n = name.toLowerCase();
		if (n.contains("pixel")) {
			return new Device(412, 2.625, ANDROID_USER_AGENT);
		}
		if (n.contains("16 pro max")) {
			return new Device(440, 3, IPHONE_USER_AGENT);
		}
		return new Device(390, 3, IPHONE_USER_AGENT); // iPhone 12 Pro and other iPhones
	}

	// Window as wide as the phone and as tall as your usable screen (taskbar excluded), at the top-left.
	// Returns the height of the visible page area once Chrome has finished resizing.
	private int sizeWindow(Device device) {
		int height = GraphicsEnvironment.isHeadless() ? 950
				: GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		driver.manage().window().setPosition(new Point(0, 0));
		driver.manage().window().setSize(new Dimension(device.width() + 16, height));

		// Chrome resizes a moment later, so wait until the visible size stops changing before measuring it
		JavascriptExecutor js = (JavascriptExecutor) driver;
		int[] last = { -1, -1 };
		int[] size = new FluentWait<WebDriver>(driver)
				.withTimeout(Duration.ofSeconds(8))
				.pollingEvery(Duration.ofMillis(400))
				.until(d -> {
					int w = ((Number) js.executeScript("return window.innerWidth;")).intValue();
					int h = ((Number) js.executeScript("return window.innerHeight;")).intValue();
					boolean stable = w == last[0] && h == last[1];
					last[0] = w;
					last[1] = h;
					return stable ? new int[] { w, h } : null;
				});
		return size[1];
	}

	// Makes the page behave like the chosen phone: its real screen width, and a screen height equal to the visible
	// browser area, so the whole phone screen is on display and nothing hangs below the window.
	private void emulateMobile(Device device, int visibleHeight) {
		Map<String, Object> metrics = new HashMap<>();
		metrics.put("width", device.width());
		metrics.put("height", visibleHeight);
		metrics.put("deviceScaleFactor", device.scale());
		metrics.put("mobile", true);

		ChromeDriver chrome = (ChromeDriver) driver;
		chrome.executeCdpCommand("Emulation.setDeviceMetricsOverride", metrics);
		chrome.executeCdpCommand("Emulation.setTouchEmulationEnabled", Map.of("enabled", true));
		chrome.executeCdpCommand("Emulation.setUserAgentOverride", Map.of("userAgent", device.userAgent()));
	}
}