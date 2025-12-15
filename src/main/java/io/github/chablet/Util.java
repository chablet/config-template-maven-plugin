package io.github.chablet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Util {
	private static final Pattern CONTENT_PATTERN = Pattern.compile("^\\{([a-zA-Z_0-9: \\.,-{}]+)\\}$");

	private Util() {
		throw new AssertionError("Utility class");
	}

	/**
	 * Loads and partitions properties from the given list of property file paths.
	 *
	 * <p>All files in {@code listOfProperties} are resolved against {@code filterDirectory}
	 * and loaded into a single {@link Properties} instance. Keys that contain exactly one
	 * dot (for example {@code env.key}) are interpreted as environment-specific properties
	 * and are placed into the returned map under the environment name ({@code env}).
	 * Keys that do not match the {@code env.key} form are treated as common properties
	 * and are copied into each environment's {@link Properties} if not already present.</p>
	 *
	 * <p>Note: properties are loaded using {@link Properties#load(java.io.InputStream)},
	 * which follows the standard ISO-8859-1 behavior for .properties streams.</p>
	 *
	 * @param basePath to locate all property files
	 * @param listOfProperties list of property file paths (resolved relative to `basePath`)
	 * @return a map from environment name to its {@link Properties} (including common properties)
	 */
	public static Map<String, Properties> loadProperties(File basePath, List<String> listOfProperties) throws IOException {
		Properties common = new Properties();
		Path base = basePath.toPath();
		Map<String, Properties> environments = new HashMap<>();

		//load all properties
		for (String propertiesFile : listOfProperties) {
			try (InputStream inputStream = Files.newInputStream(base.resolve(propertiesFile))) {
				Properties pf = new Properties();
				pf.load(inputStream);
				for (Map.Entry<Object, Object> entry : pf.entrySet()) {
					String[] keys = entry.getKey().toString().split("\\.");
					if (keys.length == 2) {
						environments.computeIfAbsent(keys[0], k -> new Properties()).put(keys[1], entry.getValue());
					} else {
						common.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}

		//copy all common properties to all environments
		//environment specific has precedence
		environments.values().forEach(properties1 ->
				common.forEach((k, v) -> {
					if (!properties1.containsKey(k)) {
						properties1.put(k, v);
					}
				})
		);
		return environments;
	}

	/**
	 * Read the names of non-directory files in a subdirectory of the given {@code baseDirectory}
	 * and return them joined as a single string.
	 *
	 * <p>The {@code reference} parameter is expected in the form:
	 * {@code directory:delimiter:prefix:suffix} where:
	 * <ul>
	 *   <li>{@code directory} (required) - name of the subdirectory under {@code baseDirectory} to read files from</li>
	 *   <li>{@code delimiter} (optional) - string used to join file names; defaults to a single space</li>
	 *   <li>{@code prefix} (optional) - string prefixed to the joined result; defaults to {@code "}</li>
	 *   <li>{@code suffix} (optional) - string appended to the joined result; defaults to {@code "}</li>
	 * </ul>
	 *
	 * If the resolved directory does not exist or is not a directory, an empty string is returned.
	 * Only direct files (not subdirectories) are included; their base names are used.
	 *
	 * @param reference reference string in the form {@code directory:delimiter:prefix:suffix}
	 * @param baseDirectory base directory used to resolve the {@code directory} segment
	 * @return a single string containing the joined file names with optional prefix and suffix,
	 *         or an empty string if the directory does not exist or is not a directory
	 */
	public static String getContent(String reference, File baseDirectory) {
		//directory:separator:start:end
		String[] params = reference.split(":", -1);
		String dirName = params.length > 0 ? params[0] : "";
		if (dirName.isEmpty()) {
			return "";
		}

		Path dir = baseDirectory.toPath().resolve(dirName);
		if (!Files.isDirectory(dir)) {
			return "";
		}

		String delimiter = params.length > 1 ? params[1] : " ";
		String prefix = params.length > 2 ? params[2] : "\"";
		String suffix = params.length > 3 ? params[3] : "\"";

		try (Stream<Path> stream = Files.list(dir)) {
			Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
			if (!it.hasNext()) {
				return "";
			}

			StringBuilder sb = new StringBuilder();
			// first element: prefix + name
			sb.append(prefix).append(it.next().getFileName().toString());
			// subsequent elements: suffix + delimiter + prefix + name
			while (it.hasNext()) {
				sb.append(suffix).append(delimiter).append(prefix).append(it.next().getFileName().toString());
			}
			// close final suffix
			sb.append(suffix);
			return sb.toString();
		} catch (IOException e) {
			return "";
		}
	}

	/**
	 * Resolves property values that reference directory content and updates the provided {@code properties}
	 * in-place.
	 *
	 * <p>If a property's value matches the pattern {@code \{directory:delimiter:prefix:suffix\}}, the reference is
	 * passed to {@link #getContent(String, File)} and the property's value is replaced with the returned string.
	 * {@link #getContent(String, File)} may return an empty string on error or when the referenced
	 * directory does not exist.</p>
	 *
	 * @param properties the {@link Properties} to process (modified in-place)
	 * @param targetDirectory base directory used to resolve references passed to {@link #getContent(String, File)}
	 */
	public static void processValues(Properties properties, File targetDirectory) {
		Map<Object, Object> updates = null;
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			Object value = entry.getValue();
			Matcher matcher = CONTENT_PATTERN.matcher(value.toString());
			if (!matcher.matches()) {
				continue;
			}
			String replacement = getContent(matcher.group(1), targetDirectory);
			if (updates == null) {
				updates = new HashMap<>();
			}
			updates.put(entry.getKey(), replacement);
		}

		if (updates != null) {
			properties.putAll(updates);
		}
	}
}
