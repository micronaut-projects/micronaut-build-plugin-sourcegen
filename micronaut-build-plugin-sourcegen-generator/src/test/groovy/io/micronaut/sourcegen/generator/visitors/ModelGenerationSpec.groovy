package io.micronaut.sourcegen.generator.visitors

class ModelGenerationSpec extends AbstractGenerationSpec {

    void "test generate with an enum model"() {
        when:
        var files = generateSources("test.Jaguar", """
        package test;
        import io.micronaut.sourcegen.annotations.*;

        @GenerateGradlePlugin(
            micronautPlugin = false,
            tasks = @GenerateGradlePlugin.GenerateGradleTask(
                source = "test.Jaguar"
            )
        )
        @PluginTask
        public record Jaguar(
            @PluginTaskParameter(defaultValue = "GOLDEN")
            Color color
        ) {

            @PluginTaskExecutable
            public void meow() {
            }

        }

        /**
         * An enum representing Jaguar's color.
         */
        enum Color {
            MELANISTIC,
            ERYTHRISM,
            GOLDEN,
            WHITE
        }
        """)

        then:
        var enumContent = stripImports(files.get("test.model.Color").getCharContent(false))
        enumContent == """/**
 * An enum representing Jaguar's color.
 */
public enum Color {

  /**
   * MELANISTIC value
   */
  MELANISTIC,
  /**
   * ERYTHRISM value
   */
  ERYTHRISM,
  /**
   * GOLDEN value
   */
  GOLDEN,
  /**
   * WHITE value
   */
  WHITE
}"""

        var taskContent = stripImports(files.get("test.JaguarTask").getCharContent(false))
        taskContent.contains("""/**
   * The work action that actually runs the task logic.
   */
  public abstract static class JaguarWorkAction implements WorkAction<JaguarWorkActionParameters> {
    Color convertColor(test.model.Color value) {
      if (value == null) {
        return null;
      } else {
        return Color.valueOf(value.name());
      }
    }

    public void execute() {
      JaguarWorkActionParameters parameters = this.getParameters();
      Color colorParam = this.convertColor(parameters.getColor().get());
      Jaguar task = new test.Jaguar(colorParam);
      task.meow();
    }
  }
""")
    }

    void "test generate with a record model"() {
        when:
        var files = generateSources("test.Jaguar", """
        package test;
        import io.micronaut.sourcegen.annotations.*;

        @GenerateGradlePlugin(
            micronautPlugin = false,
            tasks = @GenerateGradlePlugin.GenerateGradleTask(
                source = "test.Jaguar"
            )
        )
        @PluginTask
        public record Jaguar(
            @PluginTaskParameter(required = true)
            Tail tail
        ) {

            @PluginTaskExecutable
            public void meow() {
            }

        }

        /**
         * A record representing Jaguar's tail.
         *
         * @param description Detailed tail description
         * @param length The length of the tail
         * @param color The color
         */
        record Tail(
                String description,
                float length,
                Color color
        ) {
        }

        /**
         * An enum representing Jaguar's color.
         */
        enum Color {
            MELANISTIC,
            ERYTHRISM,
            GOLDEN,
            WHITE
        }
        """)

        then:
        var recordContent = stripImports(files.get("test.model.TailSpec").getCharContent(false))
        recordContent == """/**
 * A record representing Jaguar's tail.
 */
public interface TailSpec {
  /**
   * Detailed tail description.
   */
  @Input
  @Optional
  Property<String> getDescription();

  /**
   * The length of the tail.
   */
  @Input
  @Optional
  Property<Float> getLength();

  /**
   * The color.
   */
  @Input
  @Optional
  Property<Color> getColor();

  default void copyTo(TailSpec arg1) {
    arg1.getDescription().convention(this.getDescription().getOrNull());
    arg1.getLength().convention(this.getLength().getOrNull());
    arg1.getColor().convention(this.getColor().getOrNull());
  }
}"""

        var enumContent = stripImports(files.get("test.model.Color").getCharContent(false))
        enumContent != null

        var taskContent = stripImports(files.get("test.JaguarTask").getCharContent(false))
        taskContent.contains("""/**
   * The work action that actually runs the task logic.
   */
  public abstract static class JaguarWorkAction implements WorkAction<JaguarWorkActionParameters> {
    Color convertColor(test.model.Color value) {
      if (value == null) {
        return null;
      } else {
        return Color.valueOf(value.name());
      }
    }

    Tail convertTail(TailSpec value) {
      if (value == null) {
        return null;
      } else {
        Color ColorParam = this.convertColor(value.getColor().getOrNull());
        Tail result = new test.Tail(value.getDescription().getOrNull(), value.getLength().getOrNull(), ColorParam);
        return result;
      }
    }

    public void execute() {
      JaguarWorkActionParameters parameters = this.getParameters();
      Tail tailParam = this.convertTail(parameters.getTail());
      Jaguar task = new test.Jaguar(tailParam);
      task.meow();
    }
  }
""")
    }

}
