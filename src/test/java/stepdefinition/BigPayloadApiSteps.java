package stepdefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.json.Json;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import utils.ConfigReader;

// POST request with a big JSON payload and assertions on the answer. No browser is used.
// The URL is an echo service (httpbin.org/post): it sends back what it received, so we can check
// that nothing was lost or changed on the way. The URL and the limits are in config.properties.
// httpbin refuses a JSON body with more than 10000 values (413), so the payload is made big with long texts
// and a moderate number of items (about 10 values per item), not with thousands of small values.
public class BigPayloadApiSteps {

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	private final Json json = new Json();

	private Map<String, Object> payload;
	private String requestBody;
	private HttpResponse<String> response;
	private Map<String, Object> echoed; // the "json" part of the answer = the payload as the server received it
	private double secondsTaken;

	// ---------- steps ----------

	@Given("a big JSON payload with {int} items is prepared")
	public void a_big_json_payload_with_items_is_prepared(Integer itemCount) {
		List<Map<String, Object>> items = new ArrayList<>();
		for (int i = 1; i <= itemCount; i++) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", i);
			item.put("sku", "SKU-" + String.format("%06d", i));
			item.put("name", "Test product number " + i);
			item.put("description", "Long description for item " + i + ". " + "Lorem ipsum dolor sit amet. ".repeat(20));
			item.put("quantity", (i % 5) + 1);
			item.put("priceInFils", 1000 + i); // whole numbers, so the totals can be compared exactly
			item.put("tags", List.of("tag-" + (i % 10), "batch-A", "automation"));
			items.add(item);
		}
		Map<String, Object> customer = new LinkedHashMap<>();
		customer.put("name", "Test User");
		customer.put("email", "testuserautomation@catchmail.io");
		customer.put("address", Map.of("city", "Dubai", "country", "AE"));

		payload = new LinkedHashMap<>();
		payload.put("orderId", "BIG-" + System.currentTimeMillis());
		payload.put("customer", customer);
		payload.put("items", items);
		requestBody = json.toJson(payload);
		System.out.println("Big payload: " + itemCount + " items, " + requestBody.getBytes(StandardCharsets.UTF_8).length / 1024 + " KB");
	}

	@When("the payload is posted to the API")
	public void the_payload_is_posted_to_the_api() throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(ConfigReader.get("bigPayloadUrl")))
				.timeout(Duration.ofSeconds(60)).header("Content-Type", "application/json")
				.header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
		long start = System.nanoTime();
		response = client.send(request, HttpResponse.BodyHandlers.ofString());
		secondsTaken = (System.nanoTime() - start) / 1_000_000_000.0;
		if (response.statusCode() == 200) {
			echoed = castMap(parse(response.body()).get("json"));
		}
	}

	@Then("the API answers with status {int}")
	public void the_api_answers_with_status(Integer status) {
		assertEquals("Unexpected status. Body starts with: " + response.body().substring(0, Math.min(200, response.body().length())),
				status.intValue(), response.statusCode());
		assertTrue("The answer should be JSON, but Content-Type is " + response.headers().firstValue("Content-Type").orElse("none"),
				response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
	}

	@Then("the response is received within {int} seconds")
	public void the_response_is_received_within_seconds(Integer seconds) {
		System.out.printf("Response time: %.2f s%n", secondsTaken);
		assertTrue("The answer took " + secondsTaken + " s, more than " + seconds + " s", secondsTaken <= seconds);
	}

	@Then("the server received all {int} items")
	public void the_server_received_all_items(Integer itemCount) {
		assertNotNull("The answer has no echoed payload", echoed);
		assertEquals("Number of items received by the server", itemCount.intValue(), items(echoed).size());
		// the raw text the server received must be exactly as long as what we sent
		String rawBody = (String) parse(response.body()).get("data");
		assertEquals("Size of the payload received by the server", requestBody.length(), rawBody.length());
	}

	@Then("the first and the last item are unchanged")
	public void the_first_and_the_last_item_are_unchanged() {
		List<Map<String, Object>> sent = items(payload);
		List<Map<String, Object>> received = items(echoed);
		assertEquals("orderId", payload.get("orderId"), echoed.get("orderId"));
		for (int index : new int[] { 0, sent.size() - 1 }) {
			assertEquals("sku of item " + index, sent.get(index).get("sku"), received.get(index).get("sku"));
			assertEquals("description of item " + index, sent.get(index).get("description"), received.get(index).get("description"));
			assertEquals("tags of item " + index, sent.get(index).get("tags"), received.get(index).get("tags"));
		}
	}

	@Then("the order total received equals the order total sent")
	public void the_order_total_received_equals_the_order_total_sent() {
		assertEquals("Order total (quantity x price of all items)", total(items(payload)), total(items(echoed)));
	}

	// ---------- helpers ----------

	private long total(List<Map<String, Object>> items) {
		long sum = 0;
		for (Map<String, Object> item : items) {
			sum += ((Number) item.get("quantity")).longValue() * ((Number) item.get("priceInFils")).longValue();
		}
		return sum;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> items(Map<String, Object> order) {
		return (List<Map<String, Object>>) order.get("items");
	}

	private Map<String, Object> parse(String text) {
		Map<String, Object> map = json.toType(text, Json.MAP_TYPE);
		return map;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> castMap(Object value) {
		return (Map<String, Object>) value;
	}
}