package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import utils.WaitUtils;

// Cart page.  NOTE: all locators are real (istyle.ae).
public class CartPage {

	private final WaitUtils wait;

	// Real locators (istyle.ae cart): the quantity box and its + button
	By quantity = By.xpath("//input[contains(@class,'quantity__input')]");
	By plusButton = By.xpath("//button[@name='plus']");
	By checkoutButton = By.id("checkout1");

	public CartPage(WebDriver driver) {
		this.wait = new WaitUtils(driver);
	}

	public String getItemName() {
		// the quantity box is labelled "Quantity for <product name>"
		return wait.visible(quantity).getAttribute("aria-label").replace("Quantity for ", "").trim();
	}

	public int getQuantity() {
		return Integer.parseInt(wait.visible(quantity).getAttribute("value").trim());
	}

	public void increaseQuantity() {
		int before = getQuantity();
		wait.click(plusButton);
		// the number is updated by the page a moment after the click, so wait (max 10 s) until it changes
		for (int i = 0; i < 10 && getQuantity() == before; i++) {
			WaitUtils.pause(1);
		}
	}

	public void proceedToCheckout() {
		wait.click(checkoutButton);
	}
}