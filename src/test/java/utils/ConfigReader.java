package utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

	private static final Properties props = new Properties();

	static {
		try (InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
			props.load(in);
		} catch (Exception e) {
			throw new RuntimeException("Could not load config.properties", e);
		}
	}

	// A value passed with -Dkey=value overrides the file (used by Jenkins)
	public static String get(String key) {
		String fromSystem = System.getProperty(key);
		if (fromSystem != null && !fromSystem.isBlank() && !fromSystem.startsWith("${")) {
			return fromSystem;
		}
		return props.getProperty(key);
	}
}
