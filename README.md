# Configuration Template Maven plugin
The Configuration Template Plugin creates configuration to the output directory based on templates.

This plugin aims to avoid errors when maintaining application's external configuration for multiple environments. This also allows to define a configuration item (property) for all environments in a single file. This plugin is generally used with the `maven-assembly-plugin` to package the resulting configuration.

## Usage

Generate configuration using template resources

    mvn config-template:make-config

Include the plugin in your project

    <project>
      ...
      <build>
        <plugins>
          <plugin>
            <groupId>io.github.chablet</groupId>
            <artifactId>config-template-maven-plugin</artifactId>
            <configuration>
              <templates>
                <resource>
                  <directory>${project.basedir}/src/main/configuration/templates</directory>
                </resource>
              </templates>
              <filters>
                <filter>config.properties</filter>
              </filters>
            </configuration>
            <executions>
              <execution>
                <phase>generate-resources</phase>
                <goals>
                  <goal>make-config</goal>
                </goals>
              </execution>
            </executions>
          </plugin>
        </plugins>
      </build>
      ...
    </project>

See [plugin documentation](https://chablet.github.io/config-template-maven-plugin/) about parameters and examples.

