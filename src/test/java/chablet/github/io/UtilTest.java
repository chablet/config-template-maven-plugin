package chablet.github.io;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.github.chablet.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilTest {
	private final Path basePath = Path.of("src/test");

	@Test
	void loadProperties() throws IOException {
		List<String> properties = new ArrayList<>(2);
		properties.add("file1.properties");
		properties.add("file2.properties");

		Map<String, Properties> envs = Util.loadProperties(basePath.resolve("resources"), properties);

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
		//none
		assertEquals("", Util.getContent("", basePath));
		//default
		assertEquals("\"file1.properties\" \"file2.properties\"", Util.getContent("resources", basePath));
		//comma separated
		assertEquals("\"file1.properties\",\"file2.properties\"", Util.getContent("resources:,", basePath));
		//empty prefix and suffix
		assertEquals("file1.properties,file2.properties", Util.getContent("resources:,::", basePath));
		//custom delimiter, prefix and suffix
		assertEquals("{file1.properties};{file2.properties}", Util.getContent("resources:;:{:}", basePath));

		//invalid path
		assertEquals("", Util.getContent("abc:;:':'", basePath));
	}

	@Test
	void processValues() {
		Properties properties = new Properties(2);
		properties.put("a", "b");
		properties.put("c", "{resources:,}");

		Util.processValues(properties, basePath);

		assertEquals("b", properties.get("a"));
		assertEquals("\"file1.properties\",\"file2.properties\"", properties.get("c"));
	}
}
