package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;

// Generates unique data for every run
public class TestData {

	// Next email in the sequence: testuserautomation1@catchmail.io, testuserautomation2@catchmail.io, ...
	// The last used number is saved in a small file (emailCounterFile) so the next run continues from there.
	// To start again from another number, delete that file and set emailStartNumber in config.properties.
	public static String nextEmail() {
		Path counterFile = Path.of(ConfigReader.get("emailCounterFile"));
		try {
			int next = Files.exists(counterFile)
					? Integer.parseInt(Files.readString(counterFile).trim()) + 1
					: Integer.parseInt(ConfigReader.get("emailStartNumber"));
			Files.writeString(counterFile, String.valueOf(next));
			return ConfigReader.get("emailPrefix") + next + "@" + ConfigReader.get("emailDomain");
		} catch (IOException | NumberFormatException e) {
			throw new IllegalStateException("Could not read or update the email counter file " + counterFile, e);
		}
	}

	// UAE mobile number: +971, a mobile prefix (50, 52, 54, 55, 56 or 58) and 7 more digits, e.g. +971501234567
	public static String uniquePhone() {
		String[] prefixes = { "50", "52", "54", "55", "56", "58" };
		String prefix = prefixes[ThreadLocalRandom.current().nextInt(prefixes.length)];
		return "+971" + prefix + ThreadLocalRandom.current().nextInt(1000000, 9999999);
	}
}