@istyle @automated
Feature: iStyle mobile checkout flow

@smoke @checkout
Scenario: Create account with email OTP, search iPhone, add to cart, increase quantity, add shipping address and pay with a test card
Given User opens the site
When User creates an account and verifies the email OTP
And User searches for the product
And User opens product number 2 from the results
And User clicks add to cart on the product page
And User increases the quantity by one in the cart
And User navigates to the shipping address
And User fills the shipping address
And User pays with the test card
Then Payment is declined with an error message