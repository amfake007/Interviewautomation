package utils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.FluentWait;

// Reads the registration OTP from a catchmail.io mailbox using its public web API.
// No browser, no captcha, no login: any address like name@catchmail.io is a ready mailbox.
// API docs: https://catchmail.io/docs  (limit: 1 request per second, so calls are spaced out)
@SuppressWarnings("unchecked")
public class OtpReader {

	private static final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	private static final Json json = new Json();

	// OTPs already returned, so the same code is never used twice
	private static final Set<String> usedOtps = new HashSet<>();

	private static long lastCallTime = 0;

	// Waits for the OTP email of this address and returns the code
	public static String getOtp(String email) {
		Pattern otpPattern = Pattern.compile(ConfigReader.get("otpRegex"));
		long waitSeconds = Long.parseLong(ConfigReader.get("otpWaitSeconds"));

		try {
			// check the mailbox every 3 seconds until an OTP shows up or the time runs out
			String otp = new FluentWait<String>(email)
					.withTimeout(Duration.ofSeconds(waitSeconds))
					.pollingEvery(Duration.ofSeconds(3))
					.ignoring(IllegalStateException.class)
					.until(e -> findOtp(e, otpPattern));
			usedOtps.add(otp);
			return otp;
		} catch (TimeoutException e) {
			throw new AssertionError("OTP was not received within " + waitSeconds + " seconds for " + email, e);
		}
	}

	// Looks through the newest mails and returns the first unused OTP, or null if there is none yet
	private static String findOtp(String email, Pattern otpPattern) {
		String api = ConfigReader.get("catchmailApiUrl");
		String mailbox = URLEncoder.encode(email, StandardCharsets.UTF_8);

		Map<String, Object> inbox = json.toType(get(api + "/api/v1/mailbox?address=" + mailbox), Json.MAP_TYPE);
		Object listed = inbox.get("messages");
		if (listed == null) {
			return null;
		}

		// newest first
		List<Map<String, Object>> messages = new ArrayList<>();
		for (Object item : (List<Object>) listed) {
			messages.add((Map<String, Object>) item);
		}
		messages.sort(Comparator.comparing((Map<String, Object> m) -> String.valueOf(m.get("date"))).reversed());

		for (int i = 0; i < Math.min(messages.size(), 5); i++) {
			String id = String.valueOf(messages.get(i).get("id"));
			Map<String, Object> mail = json.toType(
					get(api + "/api/v1/message/" + URLEncoder.encode(id, StandardCharsets.UTF_8) + "?mailbox=" + mailbox),
					Json.MAP_TYPE);

			Matcher m = otpPattern.matcher(bodyText(mail));
			while (m.find()) {
				if (!usedOtps.contains(m.group())) {
					return m.group();
				}
			}
		}
		return null;
	}

	// Plain-text body, or the HTML body with its tags removed when there is no plain text
	private static String bodyText(Map<String, Object> mail) {
		Object bodyObject = mail.get("body");
		if (!(bodyObject instanceof Map)) {
			return "";
		}
		Map<String, Object> body = (Map<String, Object>) bodyObject;
		String text = body.get("text") == null ? "" : String.valueOf(body.get("text"));
		if (!text.isBlank()) {
			return text;
		}
		String html = body.get("html") == null ? "" : String.valueOf(body.get("html"));
		return html.replaceAll("<[^>]+>", " ");
	}

	// The API allows 1 request per second, so make sure at least 1.1 seconds pass between calls
	private static String get(String url) {
		try {
			long wait = 1100 - (System.currentTimeMillis() - lastCallTime);
			if (wait > 0) {
				Thread.sleep(wait);
			}
			lastCallTime = System.currentTimeMillis();

			HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).GET().build();
			HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 300) {
				throw new IllegalStateException("Catchmail returned " + response.statusCode() + " for " + url + ": " + response.body());
			}
			return response.body();
		} catch (IOException e) {
			throw new IllegalStateException("Could not reach catchmail: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while waiting for the OTP", e);
		}
	}
}