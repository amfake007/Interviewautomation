package pages;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import utils.WaitUtils;

// Search box + results list.  NOTE: search icon and box are real (istyle.ae); the result tiles are real too.
public class SearchPage {

	private final WebDriver driver;
	private final WaitUtils wait;

	// istyle.ae: the header search icon opens a search window with the box inside
	By searchIcon = By.xpath("//summary[@aria-label='Search']");
	By searchBoxes = By.cssSelector("input[name='q'][type='search']");
	// Real locators (istyle.ae results page): every product is a "search-product-card" tile
	By productCards = By.xpath("//div[contains(@class,'search-product-card')]");
	By productLink = By.xpath(".//a[contains(@class,'full-unstyled-link')]");
	By productName = By.xpath(".//*[contains(@class,'apl-section-product-title')]");

	public SearchPage(WebDriver driver) {
		this.driver = driver;
		this.wait = new WaitUtils(driver);
	}

	public void search(String text) {
		By box = openSearchBox();
		wait.type(box, text);
		driver.findElement(box).sendKeys(org.openqa.selenium.Keys.ENTER);
	}

	// Opens the search window and returns a locator for the search box that is really on screen.
	// The page can contain more than one search box (e.g. one per header), so the visible one is marked
	// with an attribute and found through that. If the first click did not open it (or closed it again),
	// the icon is clicked once more.
	private By openSearchBox() {
		for (int attempt = 1; attempt <= 2; attempt++) {
			wait.clickFirstVisible(searchIcon);
			long end = System.currentTimeMillis() + 5000;
			while (System.currentTimeMillis() < end) {
				for (WebElement box : driver.findElements(searchBoxes)) {
					if (box.isDisplayed()) {
						((org.openqa.selenium.JavascriptExecutor) driver)
								.executeScript("arguments[0].setAttribute('data-qa-search','true');", box);
						return By.cssSelector("[data-qa-search='true']");
					}
				}
				WaitUtils.pause(1);
			}
		}
		throw new AssertionError("The search box did not open after clicking the search icon");
	}

	public int resultCount() {
		wait.visible(productCards);
		return driver.findElements(productCards).size();
	}

	// position starts at 1, so 2 = second product
	public String openProduct(int position) {
		wait.visible(productCards);
		List<WebElement> items = driver.findElements(productCards);
		WebElement tile = items.get(position - 1);
		List<WebElement> names = tile.findElements(productName);
		String name = names.isEmpty() ? "" : names.get(0).getText().trim();
		WebElement link = tile.findElement(productLink);
		((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", link);
		try {
			link.click();
		} catch (Exception e) {
			((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", link);
		}
		return name;
	}
}