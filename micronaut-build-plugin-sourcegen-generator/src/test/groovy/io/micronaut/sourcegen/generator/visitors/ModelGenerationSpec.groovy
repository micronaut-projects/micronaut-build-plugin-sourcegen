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

  MELANISTIC,
  ERYTHRISM,
  GOLDEN,
  WHITE
}"""

        var taskContent = stripImports(files.get("test.JaguarTask").getCharContent(false))
        taskContent.contains("""public abstract static class JaguarWorkAction implements WorkAction<JaguarWorkActionParameters> {
    Color convertColor(test.model.Color value) {
      if (value == (test.model.Color) (null)) {
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
        var recordContent = stripImports(files.get("test.model.Tail").getCharContent(false))
        recordContent == """/**
 * A record representing Jaguar's tail.
 */
public class Tail implements Serializable {
  /**
   * Detailed tail description.
   */
  private String description;

  /**
   * The length of the tail.
   */
  private float length;

  /**
   * The color.
   */
  private Color color;

  public Tail(String description, float length, Color color) {
    this.description = description;
    this.length = length;
    this.color = color;
  }

  public Tail() {
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public float getLength() {
    return this.length;
  }

  public void setLength(float length) {
    this.length = length;
  }

  public Color getColor() {
    return this.color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  /**
   * Create a copy and set description.
   * Detailed tail description.
   */
  public Tail withDescription(String description) {
    return new test.model.Tail(description, this.length, this.color);
  }

  /**
   * Create a copy and set length.
   * The length of the tail.
   */
  public Tail withLength(float length) {
    return new test.model.Tail(this.description, length, this.color);
  }

  /**
   * Create a copy and set color.
   * The color.
   */
  public Tail withColor(Color color) {
    return new test.model.Tail(this.description, this.length, color);
  }
}"""

        var enumContent = stripImports(files.get("test.model.Color").getCharContent(false))
        enumContent != null

        var taskContent = stripImports(files.get("test.JaguarTask").getCharContent(false))
        taskContent.contains("""public abstract static class JaguarWorkAction implements WorkAction<JaguarWorkActionParameters> {
    Color convertColor(test.model.Color value) {
      if (value == (test.model.Color) (null)) {
        return null;
      } else {
        return Color.valueOf(value.name());
      }
    }

    Tail convertTail(test.model.Tail value) {
      if (value == (test.model.Tail) (null)) {
        return null;
      } else {
        Color ColorParam = this.convertColor(value.getColor());
        Tail result = new test.Tail(value.getDescription(), value.getLength(), ColorParam);
        return result;
      }
    }

    public void execute() {
      JaguarWorkActionParameters parameters = this.getParameters();
      Tail tailParam = this.convertTail(parameters.getTail().get());
      Jaguar task = new test.Jaguar(tailParam);
      task.meow();
    }
  }
""")
    }

}
