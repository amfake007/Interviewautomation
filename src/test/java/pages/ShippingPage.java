package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import utils.WaitUtils;

// Checkout (istyle.ae, Shopify one-page checkout): delivery method, shipping address and card payment. All locators are real.
public class ShippingPage {

	private final WebDriver driver;
	private final WaitUtils wait;

	// delivery method
	By homeDelivery = By.xpath("//label[.//strong[normalize-space()='Home Delivery']]");

	// address form. The form also has hidden 'autofill' copies of some fields with the same name, so those are skipped.
	By country = By.xpath("//select[@name='countryCode']/option[@selected]");
	By firstName = By.xpath("//input[@name='firstName' and not(@data-autofill-honeypot)]");
	By lastName = By.xpath("//input[@name='lastName' and not(@data-autofill-honeypot)]");
	By address1 = By.xpath("//input[@name='address1' and not(@data-autofill-honeypot)]");
	By address2 = By.name("address2");
	By area = By.xpath("//input[@name='city' and not(@data-autofill-honeypot)]");
	By emirate = By.xpath("//select[@name='zone']");
	By phone = By.name("phone");

	// payment: the card details are inside separate iframes (Shopify keeps card data apart from the page)
	By creditCardOption = By.xpath("//label[@for='basic-creditCards']");
	By cardNumberFrame = By.xpath("//iframe[@title='Card number']");
	By cardNameFrame = By.xpath("//iframe[@title='Name on card']");
	By cardInput = By.cssSelector("input");
	By payNowButton = By.id("checkout-pay-button");
	By paymentError = By.xpath("//*[contains(text(),'issue processing your payment')]");

	public ShippingPage(WebDriver driver) {
		this.driver = driver;
		this.wait = new WaitUtils(driver);
	}

	// The checkout page is open when the 'Home Delivery' choice is there
	public boolean isShippingPageDisplayed() {
		return wait.isDisplayed(homeDelivery);
	}

	public void selectHomeDelivery() {
		wait.click(homeDelivery);
	}

	// Country/Region is already set by the site, so it is only read (not changed)
	public String getSelectedCountry() {
		return wait.visibleOrPresent(country).getText().trim();
	}

	public void fillAddress(String first, String last, String line1, String line2, String areaName,
			String emirateValue, String phoneNumber) {
		wait.type(firstName, first);
		wait.type(lastName, last);
		wait.type(address1, line1);
		wait.type(address2, line2);
		wait.type(area, areaName);
		wait.selectByValue(emirate, emirateValue);
		wait.type(phone, phoneNumber);
	}

	// Chooses 'Credit or Debit Card' (already chosen by default), fills the card and clicks Pay now.
	// The card number is typed into its iframe. The expiry and the security code are then typed like a person would:
	// the Tab key moves from one card field to the next, and the keys go to whichever field has the focus.
	public void payWithCard(String number, String expiry, String cvv, String nameOnCard) {
		wait.click(creditCardOption);
		typeInFrame(cardNumberFrame, number);
		sleep(700);
		pressTab();
		typeKeys(expiry);
		sleep(700);
		pressTab();
		typeKeys(cvv);
		typeInFrame(cardNameFrame, nameOnCard);
		wait.click(payNowButton);
	}

	private void pressTab() {
		new Actions(driver).sendKeys(Keys.TAB).perform();
		sleep(500);
	}

	// Types into whichever field has the focus, one key at a time
	private void typeKeys(String text) {
		for (char c : text.toCharArray()) {
			new Actions(driver).sendKeys(String.valueOf(c)).perform();
			sleep(150);
		}
	}

	// Steps into the card iframe, types into its single input one character at a time (slowly, because Shopify's
	// card fields format the text while you type), and always steps back out. If the value is not what was typed
	// the field is tried again (3 times). A card field can hide its value from the test, so after the last
	// try the test carries on: a wrong card would show up as a different message at the end.
	private void typeInFrame(By frame, String value) {
		String wanted = value.replaceAll("[\\s\\-()/]", "");
		for (int attempt = 1; attempt <= 3; attempt++) {
			WaitUtils.pause(1); // let the card fields finish redrawing (they redraw after each field is filled)
			try {
				WebElement f = wait.visibleOrPresent(frame);
				((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", f);
				driver.switchTo().frame(f);
				WebElement input = driver.findElement(cardInput);
				input.click();
				input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
				for (char c : value.toCharArray()) {
					driver.findElement(cardInput).sendKeys(String.valueOf(c));
					sleep(150);
				}
				String now = driver.findElement(cardInput).getAttribute("value");
				if (now != null && wanted.equals(now.replaceAll("[\\s\\-()/]", ""))) {
					return;
				}
			} catch (WebDriverException e) {
				// the frame was redrawn while typing: find it again and retry
			} finally {
				driver.switchTo().defaultContent();
			}
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// The test card is declined, so the site shows an error. Waits up to the given seconds for it.
	public boolean isPaymentErrorDisplayed(long seconds) {
		return wait.isDisplayedWithin(paymentError, seconds);
	}
}