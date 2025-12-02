package io.github.chablet;

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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Build configuration using templates. Allows to include static content.
 *
 * @author <a href="chablet@outlook.com">Miguel Bautista</a>
 */
@Mojo(name = "make-config", defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ConfigTemplate extends AbstractMojo {

	/**
	 * The character encoding to use when reading and writing filtered resources.
	 */
	@Parameter(defaultValue = "${project.build.sourceEncoding}")
	protected String encoding;

	/**
	 * The character encoding to use when reading and writing filtered properties files.
	 * If not specified, it will default to the value of the "encoding" parameter.
	 */
	@Parameter
	protected String propertiesEncoding;

	/**
	 * Location of the properties file to source the templates.
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/resources")
	protected File filterDirectory;

	/**
	 * The list of template resources to process.
	 */
	@Parameter(required = true)
	protected List<Resource> templates;

	/**
	 * The list of static resources to copy.
	 */
	@Parameter
	protected List<Resource> staticResources;

	/**
	 * List of properties files.
	 */
	@Parameter(required = true)
	protected List<String> filters;

	/**
	 * Location of resulting configuration.
	 */
	@Parameter(property = "targetDirectory", defaultValue = "${project.build.directory}/configuration")
	protected File targetDirectory;

	/**
	 * Flag to overwrite configuration.
	 */
	@Parameter(property = "overwrite", defaultValue = "true")
	protected boolean overwrite = true;

	/**
	 * Copy any empty directories included in the Resources.
	 */
	@Parameter(defaultValue = "false")
	protected boolean includeEmptyDirs;

	/**
	 * Whether to escape backslashes and colons in windows-style paths.
	 */
	@Parameter(defaultValue = "true")
	protected boolean escapeWindowsPaths;

	/**
	 * <p>
	 * Set of delimiters for expressions to filter within the resources. These delimiters are specified in the form
	 * {@code beginToken*endToken}. If no {@code *} is given, the delimiter is assumed to be the same for start and end.
	 * </p>
	 * <p>
	 * So, the default filtering delimiters might be specified as:
	 * </p>
	 *
	 * <pre>
	 * &lt;delimiters&gt;
	 *   &lt;delimiter&gt;${*}&lt;/delimiter&gt;
	 *   &lt;delimiter&gt;@&lt;/delimiter&gt;
	 * &lt;/delimiters&gt;
	 * </pre>
	 * <p>
	 * Since the {@code @} delimiter is the same on both ends, we don't need to specify {@code @*@} (though we can).
	 * </p>
	 */
	@Parameter
	protected LinkedHashSet<String> delimiters;

	/**
	 * Use default delimiters in addition to custom delimiters, if any.
	 */
	@Parameter(defaultValue = "true")
	protected boolean useDefaultDelimiters;

	/**
	 * Expressions preceded with this string won't be interpolated. Anything else preceded with this string will be
	 * passed through unchanged. For example {@code \${foo}} will be replaced with {@code ${foo}} but {@code \\${foo}}
	 * will be replaced with {@code \\value of foo}, if this parameter has been set to the backslash.
	 */
	@Parameter
	protected String escapeString;

	private final MavenResourcesFiltering mavenResourcesFiltering;

	private final MavenSession session;

	private final MavenProject project;

	/**
	 * Constructor to pass maven context information.
	 * @param mavenResourcesFiltering context
	 * @param session context
	 * @param project context
	 */
	@Inject
	public ConfigTemplate(MavenResourcesFiltering mavenResourcesFiltering, MavenSession session, MavenProject project) {
		this.mavenResourcesFiltering = mavenResourcesFiltering;
		this.session = session;
		this.project = project;
	}

	/** {@inheritDoc} */
	public void execute() throws MojoExecutionException {
		if (templates == null || templates.isEmpty()) {
			getLog().info("No templates, skipping the execution.");
			return;
		}

		if (encoding == null || encoding.isEmpty()) {
			getLog().warn("File encoding has not been set, using platform encoding "
					+ System.getProperty("file.encoding")
					+ ". Build is platform dependent!");
			getLog().warn("See https://maven.apache.org/general.html#encoding-warning");
		}

		Map<String, Properties> envProperties = loadProperties(filters);

		for (Map.Entry<String, Properties> environment : envProperties.entrySet()) {
			File environmentFolder = targetDirectory.toPath().resolve(environment.getKey()).toFile();
			copyStatic(staticResources, environmentFolder);
			generateConfiguration(templates, environment.getValue(), environmentFolder);
		}

	}



	private Map<String, Properties> loadProperties(List<String> listOfProperties) throws MojoExecutionException {
		Properties properties = new Properties();
		//load all properties
		for (String propertiesFile : listOfProperties) {
			try (InputStream inputStream = Files.newInputStream(filterDirectory.toPath().resolve(propertiesFile))) {
				Properties pf = new Properties();
				pf.load(inputStream);
				properties.putAll(pf);
			} catch (IOException e) {
				throw new MojoExecutionException(e);
			}
		}

		//split by environment
		Properties common = new Properties();
		Map<String, Properties> environments = new HashMap<>();
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String[] keys = entry.getKey().toString().split("\\.");
			if (keys.length != 2) {
				common.put(entry.getKey(), entry.getValue());
				continue;
			}
			Properties envProperties = environments.get(keys[0]);
			if (envProperties == null) {
				envProperties = new Properties();
				environments.put(keys[0], envProperties);
			}
			envProperties.put(keys[1], entry.getValue());
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

	private void generateConfiguration(List<Resource> resources, Properties additionalProperties, File templateTargetDirectory) throws MojoExecutionException {
		//ensure all resources are filtered
		resources.forEach(resource -> resource.setFiltering(true));

			MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
					resources,
					templateTargetDirectory,
					project,
					encoding,
					Collections.emptyList(),
					Collections.emptyList(),
					session);

			mavenResourcesExecution.setEscapeWindowsPaths(escapeWindowsPaths);
			mavenResourcesExecution.setInjectProjectBuildFilters(false);
			mavenResourcesExecution.setEscapeString(escapeString);
			mavenResourcesExecution.setOverwrite(overwrite);
			mavenResourcesExecution.setIncludeEmptyDirs(includeEmptyDirs);
			mavenResourcesExecution.setAdditionalProperties(additionalProperties);
			mavenResourcesExecution.setDelimiters(delimiters, useDefaultDelimiters);
			mavenResourcesExecution.setPropertiesEncoding(propertiesEncoding);

		try {
			mavenResourcesFiltering.filterResources(mavenResourcesExecution);
		} catch (MavenFilteringException e) {
			throw new MojoExecutionException(e);
		}

	}

	private void copyStatic(List<Resource> resources, File templateTargetDirectory) throws MojoExecutionException {
		//ensure all resources are not filtered
		resources.forEach(resource -> resource.setFiltering(false));

			MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution(
					resources,
					templateTargetDirectory,
					project,
					encoding,
					Collections.emptyList(),
					Collections.emptyList(),
					session);

			mavenResourcesExecution.setEscapeWindowsPaths(escapeWindowsPaths);
			mavenResourcesExecution.setInjectProjectBuildFilters(false);
			mavenResourcesExecution.setOverwrite(overwrite);
			mavenResourcesExecution.setIncludeEmptyDirs(includeEmptyDirs);
		try {
			mavenResourcesFiltering.filterResources(mavenResourcesExecution);
		} catch (MavenFilteringException e) {
			throw new MojoExecutionException(e);
		}
	}
}
