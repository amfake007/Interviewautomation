package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import utils.WaitUtils;

// Product details page (PDP).  NOTE: only the title locator is still a placeholder.
public class ProductPage {

	private final WaitUtils wait;

	By title = By.cssSelector("h1");
	// Real locator (istyle.ae PDP): the Add to cart button
	By addToCartButton = By.id("footer-atc");
	// Real locators: the pop-up that opens after Add to cart has a 'View my cart (1)' link
	By addedConfirmation = By.xpath("//a[contains(@class,'button--cto-cart')]");
	By viewCartButton = By.xpath("//a[contains(@class,'button--cto-cart')]");

	public ProductPage(WebDriver driver) {
		this.wait = new WaitUtils(driver);
	}

	public String getTitle() {
		return wait.text(title);
	}

	// Clicks Add to cart. If the 'View my cart' pop-up does not show (the page can ignore a click made before its
	// scripts are ready), the button is clicked once more.
	public void addToCart() {
		wait.click(addToCartButton);
		if (!wait.isDisplayedWithin(addedConfirmation, 10)) {
			WaitUtils.pause(2);
			wait.click(addToCartButton);
		}
	}

	public boolean isAddedToCart() {
		return wait.isDisplayed(addedConfirmation);
	}

	public void openCart() {
		wait.click(viewCartButton);
	}
}