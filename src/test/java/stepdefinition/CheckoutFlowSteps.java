package stepdefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import pages.LoginSignupPage;
import pages.CartPage;
import pages.ProductPage;
import pages.SearchPage;
import pages.ShippingPage;
import utils.ConfigReader;
import utils.OtpReader;
import utils.TestData;

public class CheckoutFlowSteps {

	LoginSignupPage loginSignupPage;
	SearchPage searchPage;
	ProductPage productPage;
	CartPage cartPage;
	ShippingPage shippingPage;

	// unique user for this scenario (a new CheckoutFlowSteps object is created for every scenario)
	String email;
	String phone = TestData.uniquePhone();

	int quantityBefore;

	@Given("User opens the site")
	public void user_opens_the_site() {
		// the site is opened in the @Before hook (BaseSteps)
		loginSignupPage = new LoginSignupPage(BaseSteps.driver);
		searchPage = new SearchPage(BaseSteps.driver);
		productPage = new ProductPage(BaseSteps.driver);
		cartPage = new CartPage(BaseSteps.driver);
		shippingPage = new ShippingPage(BaseSteps.driver);
		assertTrue("Home page did not load", BaseSteps.driver.getCurrentUrl().startsWith(ConfigReader.get("baseUrl")));
	}

	// ---------- Register / Login (no password: email + code from the email) ----------

	@When("User creates an account and verifies the email OTP")
	public void user_creates_an_account_and_verifies_the_email_otp() {
		email = TestData.nextEmail();
		loginSignupPage.submitEmail(email);
		assertTrue("Continue did not ask for the code for " + email, loginSignupPage.isOtpScreenDisplayed());
		loginSignupPage.verifyOtp(OtpReader.getOtp(email));
		loginSignupPage.startShoppingIfShown();
		assertTrue("User is not logged in after the code for " + email, loginSignupPage.isLoggedIn());
	}

	// ---------- Search, product, cart ----------

	@When("User searches for the product")
	public void user_searches_for_the_product() {
		searchPage.search(ConfigReader.get("searchText"));
		assertTrue("No search results found", searchPage.resultCount() > 0);
	}

	@When("User opens product number {int} from the results")
	public void user_opens_product_number_from_the_results(Integer position) {
		assertTrue("Fewer than " + position + " products in the results", searchPage.resultCount() >= position);
		searchPage.openProduct(position);
		assertTrue("Product page title should contain iPhone",
				productPage.getTitle().toLowerCase().contains("iphone"));
	}

	@When("User clicks add to cart on the product page")
	public void user_clicks_add_to_cart_on_the_product_page() {
		String productTitle = productPage.getTitle();
		productPage.addToCart();
		assertTrue("Item was not added to the cart", productPage.isAddedToCart());
		productPage.openCart();
		assertTrue("Cart does not contain the selected product: " + productTitle,
				cartPage.getItemName().toLowerCase().contains("iphone"));
	}

	@When("User increases the quantity by one in the cart")
	public void user_increases_the_quantity_by_one_in_the_cart() {
		quantityBefore = cartPage.getQuantity();
		cartPage.increaseQuantity();
		assertEquals("Quantity did not increase by one", quantityBefore + 1, cartPage.getQuantity());
	}

	// ---------- Shipping ----------

	@When("User navigates to the shipping address")
	public void user_navigates_to_the_shipping_address() {
		cartPage.proceedToCheckout();
		assertTrue("Shipping page is not displayed", shippingPage.isShippingPageDisplayed());
	}

	@When("User fills the shipping address")
	public void user_fills_the_shipping_address() {
		shippingPage.selectHomeDelivery();
		assertEquals("Country/Region should already be set", ConfigReader.get("expectedCountry"),
				shippingPage.getSelectedCountry());
		shippingPage.fillAddress(ConfigReader.get("firstName"), ConfigReader.get("lastName"),
				ConfigReader.get("addressLine1"), ConfigReader.get("addressLine2"), ConfigReader.get("areaName"),
				ConfigReader.get("emirate"), phone);
	}

	@When("User pays with the test card")
	public void user_pays_with_the_test_card() {
		shippingPage.payWithCard(ConfigReader.get("cardNumber"), ConfigReader.get("cardExpiry"),
				ConfigReader.get("cardCvv"), ConfigReader.get("cardName"));
	}

	// The test card is not a real one, so the payment is declined. Seeing this message is the expected end of the test.
	@Then("Payment is declined with an error message")
	public void payment_is_declined_with_an_error_message() {
		assertTrue("The payment error message was not shown after Pay now",
				shippingPage.isPaymentErrorDisplayed(Long.parseLong(ConfigReader.get("paymentErrorWaitSeconds"))));
	}
}