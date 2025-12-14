package io.github.chablet;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {

	@Test
	void loadProperties() throws IOException {
		List<String> properties = new ArrayList<>(2);
		properties.add("file1.properties");
		properties.add("file2.properties");
		File basePath = new File("src/main/test/resources");

		Map<String, Properties> envs = Util.loadProperties(basePath.getAbsoluteFile(), properties);

		//total environments
		assertEquals(3, envs.size());

		Properties env = envs.get("env1");
		assertEquals(5, env.size());
		assertEquals("one", env.getProperty("general1"));

		env = envs.get("env2");
		assertEquals(5, env.size());
		assertEquals("two", env.getProperty("general2"));
		assertEquals("0.3", env.getProperty("prop3"));

		env = envs.get("env3");
		assertEquals(3, env.size());
		assertEquals("o-n-e", env.getProperty("general1"));
		assertEquals("0.3", env.getProperty("prop3"));
	}

	@Test
	void getContent() {
		File basePath = new File("src/main/test");
		//default
		assertEquals("\"file2.properties\" \"file1.properties\"", Util.getContent("resources", basePath));
		//comma separated
		assertEquals("\"file2.properties\",\"file1.properties\"", Util.getContent("resources:,", basePath));
		//empty prefix and suffix
		assertEquals("file2.properties,file1.properties", Util.getContent("resources:,::", basePath));
		//custom delimiter, prefix and suffix
		assertEquals("{file2.properties};{file1.properties}", Util.getContent("resources:;:{:}", basePath));

		//invalid path
		assertEquals("", Util.getContent("abc:;:':'", basePath));
	}

	@Test
	void processValues() {
		Properties properties = new Properties(2);
		properties.put("a", "b");
		properties.put("c", "{resources:,}");

		File basePath = new File("src/main/test");
		Util.processValues(properties, basePath);

		assertEquals("b", properties.get("a"));
		assertEquals("\"file2.properties\",\"file1.properties\"", properties.get("c"));
	}
}
