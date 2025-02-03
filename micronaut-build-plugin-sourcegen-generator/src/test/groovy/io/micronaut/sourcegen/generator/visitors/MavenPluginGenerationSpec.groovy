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
      defaultValue = "true"
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
  public void execute() {
    if (!this.enabled) {
      this.getLog().debug("WolfMojo is disabled");
    } else {
      Wolf task = new test.Wolf(this.slogan, this.age);
      task.awooo();
    }
  }
}"""
    }

}
