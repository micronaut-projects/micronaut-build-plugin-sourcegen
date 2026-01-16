package io.micronaut.sourcegen.example.plugin.maven;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

abstract class AbstractMavenPluginTest {

    @TempDir
    public File baseDir;

    public MavenProject project;

    protected PlexusContainer container;

    @BeforeEach
    public void setUp() throws PlexusContainerException {
        container = new DefaultPlexusContainer();
        container.addComponent(new BasicComponentConfigurator(), ComponentConfigurator.class, "basic");
        project = new MavenProject();
    }

    @AfterEach
    public void tearDown() {
        container.dispose();
    }

    private PlexusConfiguration extractPluginConfiguration(String artifactId, File pom) throws Exception {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileInputStream(pom));
        Plugin plugin = model.getBuild().getPlugins().stream()
            .filter(p -> p.getArtifactId().equals(artifactId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Plugin not found: " + artifactId));
        Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
        if (dom == null) {
            return new XmlPlexusConfiguration("configuration");
        }
        return new XmlPlexusConfiguration(dom);
    }

    private <T> T instantiateMojo(Class<T> mojoClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return mojoClass.getConstructor().newInstance();
    }

    public <T> T findConfiguredMojo(Class<T> mojoClass, File configurationPom) throws Exception {
        T mojo = instantiateMojo(mojoClass);
        PlexusConfiguration configuration = extractPluginConfiguration("test", configurationPom);
        configuration.addChild("outputFolder", baseDir.getAbsolutePath());
        ComponentConfigurator configurator = container.lookup(ComponentConfigurator.class, "basic");
        configurator.configureComponent(
            mojo,
            configuration,
            new CustomExpressionEvaluator(),
            container.getContainerRealm()
        );
        return mojo;
    }

    private static class CustomExpressionEvaluator implements ExpressionEvaluator {

        @Override
        public Object evaluate(String expression) {
            return expression;
        }

        @Override
        public File alignToBaseDirectory(File path) {
            return path;
        }
    }

    File file(String relativePath) {
        return baseDir.toPath().resolve(relativePath).toFile();
    }

    String content(File file) {
        try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
            return new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
