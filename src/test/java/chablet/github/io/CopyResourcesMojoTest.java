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

import io.github.chablet.ConfigTemplate;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MojoTest
class CopyResourcesMojoTest {

	@Test
	@InjectMojo(goal = "make-config", pom = "classpath:/sample/plugin-config.xml")
	@Basedir("target/test-classes")
	void copyStatic(ConfigTemplate mojo) throws MojoExecutionException {

		mojo.execute();

		Path output = Path.of("target/test-classes/output");

		//copy of static of common directory: all
		Path env1 = output.resolve("env1");
		assertTrue(env1.toFile().isDirectory());
		assertTrue(env1.resolve("all/all.txt").toFile().isFile());

		//copy of static of common file: sample.txt
		assertTrue(env1.resolve("sample.txt").toFile().isFile());


		//copy of static of common directory: all
		Path env2 = output.resolve("env2");
		assertTrue(env2.toFile().isDirectory());
		assertTrue(env2.resolve("all/all.txt").toFile().isFile());

		//copy of static of common file: sample.txt
		assertTrue(env1.resolve("sample.txt").toFile().isFile());

		//copy of static of environment specific directory: env1
		assertTrue(env1.resolve("special.txt").toFile().isFile());

		//test template1
		try (InputStream inputStream = Files.newInputStream(env1.resolve("app.properties"))) {
			Properties pf = new Properties();
			pf.load(inputStream);
			assertEquals("all-enviroments", pf.get("test1"));
			assertEquals("env1.val2", pf.get("test2"));
			assertEquals("env1.val3", pf.get("test3"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//test template2
		try (InputStream inputStream = Files.newInputStream(env2.resolve("app.properties"))) {
			Properties pf = new Properties();
			pf.load(inputStream);
			assertEquals("all-enviroments", pf.get("test1"));
			assertEquals("env2.val2", pf.get("test2"));
			assertEquals("all.txt", pf.get("test3"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
