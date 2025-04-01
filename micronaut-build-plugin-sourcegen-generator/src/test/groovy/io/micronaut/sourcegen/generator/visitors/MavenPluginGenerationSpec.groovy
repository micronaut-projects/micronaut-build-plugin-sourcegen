package io.micronaut.sourcegen.generator.visitors

class MavenPluginGenerationSpec extends AbstractGenerationSpec {

    void "test simple maven plugin generation"() {
        when:
        var files = generateSources("test.Wolf", """
        package test;
        import io.micronaut.sourcegen.annotations.*;

        @GenerateMavenMojo(
            micronautPlugin = false,
            source = "test.Wolf"
        )
        @PluginTask
        public record Wolf(
                @PluginTaskParameter(required = true)
                String slogan,
                @PluginTaskParameter(defaultValue = "1")
                Integer age
        ) {

            @PluginTaskExecutable
            public void awooo() {
            }

        }
        """)

        then:
        var mojoContent = stripImports(files.get("test.WolfMojo").getCharContent(false))
        mojoContent == """/**
 * Wolf Maven Mojo.
 */
public abstract class WolfMojo extends AbstractMojo {
  /**
   * The common prefix for Mojo properties.
   */
  protected static final String PROPERTY_PREFIX = "wolf";

  @Parameter(
      defaultValue = "\${project}",
      required = true,
      readonly = true
  )
  protected MavenProject project;

  /**
   * Determines if this mojo must be executed. The value is true if the mojo is enabled.
   */
  @Parameter(
      property = "wolf.enabled",
      defaultValue = "false"
  )
  protected boolean enabled;

  /**
   * Configurable slogan parameter.
   */
  @Parameter(
      required = true
  )
  protected String slogan;

  /**
   * Configurable age parameter.
   */
  @Parameter(
      defaultValue = "1"
  )
  protected Integer age;

  /**
   * Main execution of Wolf Mojo.
   */
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!this.enabled) {
      this.getLog().debug("WolfMojo is disabled");
    } else {
      try {
      Wolf task = new test.Wolf(this.slogan, this.age);
      task.awooo();

      } catch (IllegalArgumentException e0) {throw new org.apache.maven.plugin.MojoFailureException("Invalid configuration for Wolf", e0);

      } catch (Exception e1) {throw new org.apache.maven.plugin.MojoExecutionException("Failed to run Wolf", e1);

      }}
  }
}"""
    }

}
