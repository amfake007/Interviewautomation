@api @bigpayload @automated
Feature: POST request with a big payload

@api @bigpayload
Scenario: A big JSON payload is posted and received completely
Given a big JSON payload with 500 items is prepared
When the payload is posted to the API
Then the API answers with status 200
And the response is received within 15 seconds
And the server received all 500 items
And the first and the last item are unchanged
And the order total received equals the order total sent