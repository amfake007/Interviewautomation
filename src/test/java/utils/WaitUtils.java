package utils;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

// Reusable helpers so page classes stay short and never use Thread.sleep
public class WaitUtils {

	private final WebDriver driver;
	private final WebDriverWait wait;

	public WaitUtils(WebDriver driver) {
		this.driver = driver;
		this.wait = new WebDriverWait(driver, Duration.ofSeconds(Long.parseLong(ConfigReader.get("timeout"))));
	}

	// A plain pause of a few seconds, used to slow the run down between steps on purpose
	public static void pause(long seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// The site sometimes shows a marketing pop-up (MoEngage) at any moment. It lives inside an iframe, so we have to
	// step into that frame to press its x. Only looks (no waiting); closes the pop-up if it is shown.
	// Called before every click / type / select so the pop-up can never get in the way of the real step.
	private static final By promoFrame = By.xpath("//iframe[starts-with(@id,'moe-onsite-campaign')]");
	private static final By promoClose = By.id("close-icon");

	public void closePromoPopupIfShown() {
		try {
			for (WebElement frame : driver.findElements(promoFrame)) {
				if (frame.isDisplayed()) {
					try {
						driver.switchTo().frame(frame);
						WebElement close = driver.findElement(promoClose);
						((JavascriptExecutor) driver).executeScript("arguments[0].click();", close);
					} finally {
						driver.switchTo().defaultContent();
					}
					pause(1);
					return;
				}
			}
		} catch (Exception ignored) {
			// pop-up disappeared while we were looking at it: nothing to close
			driver.switchTo().defaultContent();
		}
	}

	public WebElement visible(By locator) {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	// Finds an element that is on the page even if it is not 'visible' (e.g. the selected <option> of a drop-down)
	public WebElement visibleOrPresent(By locator) {
		return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
	}

	public WebElement clickable(By locator) {
		return wait.until(ExpectedConditions.elementToBeClickable(locator));
	}

	public void click(By locator) {
		closePromoPopupIfShown();
		WebElement el = clickable(locator);
		try {
			el.click();
		} catch (Exception e) {
			// fallback when a banner or overlay blocks the element
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
		}
	}

	// For things the page has twice (e.g. one header icon for desktop and one for phone): waits for the first one that
	// is visible and clicks it
	public void clickFirstVisible(By locator) {
		closePromoPopupIfShown();
		WebElement el = new org.openqa.selenium.support.ui.FluentWait<>(driver)
				.withTimeout(Duration.ofSeconds(Long.parseLong(ConfigReader.get("timeout"))))
				.pollingEvery(Duration.ofMillis(500)).withMessage("No visible element for " + locator).until(d -> {
					for (WebElement e : d.findElements(locator)) {
						if (e.isDisplayed()) {
							return e;
						}
					}
					return null;
				});
		try {
			el.click();
		} catch (Exception e) {
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
		}
	}

	// Clears the field and types the text one character at a time.
	// The site redraws a field while you type in it (for example when it shows or hides the 'proper email' error),
	// which can lose the rest of the text. So the field is found again for every character, the final value is
	// checked, and the whole typing is repeated if the field does not hold exactly the text.
	public void type(By locator, String text) {
		boolean mac = System.getProperty("os.name").toLowerCase().contains("mac");
		String lastValue = "";

		for (int attempt = 1; attempt <= 4; attempt++) {
			try {
				closePromoPopupIfShown();
				WebElement field = visible(locator);
				field.click();
				field.sendKeys(Keys.chord(mac ? Keys.COMMAND : Keys.CONTROL, "a"), Keys.BACK_SPACE);

				for (char c : text.toCharArray()) {
					visible(locator).sendKeys(String.valueOf(c));
				}

				lastValue = visible(locator).getAttribute("value");
				// some fields add spaces or dashes by themselves (a phone field turns +971523205920 into +971 52 320 5920),
				// so the check ignores spaces, dashes, slashes and brackets
				if (lastValue != null && text.replaceAll("[\\s\\-()/]", "").equals(lastValue.replaceAll("[\\s\\-()/]", ""))) {
					return;
				}
			} catch (ElementClickInterceptedException e) {
				// the pop-up appeared just now and covers the field: close it and type again
				closePromoPopupIfShown();
			}
		}
		throw new AssertionError("Could not type '" + text + "' into " + locator + " (the field holds '" + lastValue + "')");
	}

	// Picks an option of a <select> drop-down by its value attribute
	public void selectByValue(By locator, String value) {
		closePromoPopupIfShown();
		new Select(visible(locator)).selectByValue(value);
	}

	// Waits until the element is gone from the page
	public boolean waitUntilGone(By locator, long seconds) {
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(seconds))
					.until(ExpectedConditions.invisibilityOfElementLocated(locator));
		} catch (Exception e) {
			return false;
		}
	}

	public String text(By locator) {
		return visible(locator).getText().trim();
	}

	// Waits up to the timeout for the element to show
	public boolean isDisplayed(By locator) {
		try {
			return visible(locator).isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	// Same as isDisplayed but with your own (shorter) wait, good for 'should NOT appear' checks
	public boolean isDisplayedWithin(By locator, long seconds) {
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(seconds))
					.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}
}