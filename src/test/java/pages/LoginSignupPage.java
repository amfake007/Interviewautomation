package pages;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import utils.ConfigReader;
import utils.WaitUtils;

// Register / Login on istyle.ae. There is no password: the same screens are used for both.
// Account icon -> email -> Continue arrow -> 6-digit code from the email -> back on the shop, logged in.
public class LoginSignupPage {

	private final WebDriver driver;
	private final WaitUtils wait;

	// header account icon (opens the login page)
	By accountIcon = By.xpath("//a[contains(@class,'header__icon--account')]");

	// email screen
	By emailField = By.id("customer-authentication-web-email");
	By continueButton = By.xpath("//button[@aria-label='Continue']");

	// code screen
	By otpHeading = By.xpath("//h1[.//span[normalize-space()='Enter code']]");
	By otpField = By.xpath("//input[@aria-label='6-digit code']");
	By otpFieldBox = By.cssSelector("[data-testid='otp-field']");

	// shown after the code is accepted
	By startShoppingButton = By.xpath("//button[normalize-space()='Start Shopping']");

	public LoginSignupPage(WebDriver driver) {
		this.driver = driver;
		this.wait = new WaitUtils(driver);
	}

	// Opens the login page from the header icon, types the email and clicks the arrow (the code screen comes next)
	public void submitEmail(String email) {
		wait.clickFirstVisible(accountIcon);
		wait.type(emailField, email);
		wait.click(continueButton);
	}

	public boolean isOtpScreenDisplayed() {
		return wait.isDisplayedWithin(otpHeading, 15);
	}

	// Types the code. istyle submits it by itself after the 6th digit; if it does not, Enter is pressed.
	public void verifyOtp(String otp) {
		WebElement field = new WebDriverWait(driver, Duration.ofSeconds(15))
				.until(ExpectedConditions.presenceOfElementLocated(otpField));
		try {
			field.sendKeys(otp);
		} catch (ElementNotInteractableException e) {
			// the real input is drawn invisibly over the 6 boxes: click the boxes and type
			new Actions(driver).click(driver.findElement(otpFieldBox)).sendKeys(otp).perform();
		}
		if (!wait.waitUntilGone(otpHeading, 8)) {
			new Actions(driver).sendKeys(Keys.ENTER).perform();
			wait.waitUntilGone(otpHeading, 10);
		}
		WaitUtils.pause(Long.parseLong(ConfigReader.get("afterVerifyWaitSeconds")));
	}

	// After login the site shows a 'Start Shopping' button. Clicks it when it appears (waits up to 10 s),
	// and carries on if it does not show.
	public void startShoppingIfShown() {
		if (wait.isDisplayedWithin(startShoppingButton, 10)) {
			wait.click(startShoppingButton);
		}
	}

	// Logged in = we are back on the shop (not on the login pages any more)
	public boolean isLoggedIn() {
		try {
			return new WebDriverWait(driver, Duration.ofSeconds(20)).until(
					d -> d.getCurrentUrl().startsWith(ConfigReader.get("baseUrl")) && !d.getCurrentUrl().contains("authentication"));
		} catch (Exception e) {
			return false;
		}
	}
}