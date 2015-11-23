package ru.vyarus.gradle.plugin.lib

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2015
 */
abstract class AbstractKitTest extends Specification {

    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        // override maven local repository
        // (see org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator.getLocalMavenRepository)
        System.setProperty("maven.repo.local", new File(testProjectDir.root, "build/repo").getAbsolutePath());
    }

    def build(String file) {
        buildFile << file
    }

    File file(String path) {
        new File(testProjectDir.root, path)
    }

    File fileFromClasspath(String toFile, String source) {
        File target = file(toFile)
        target.parentFile.mkdirs()
        target << getClass().getResourceAsStream(source).text
    }

    String projectName() {
        return testProjectDir.root.getName()
    }

    GradleRunner gradle(String... commands) {
        // tests will be called after deps resolution, so dep will be in local repo for sure
        def file = new File(System.getProperty('user.home') + '/.m2/repository/ru/vyarus/gradle-pom-plugin/1.0.0/gradle-pom-plugin-1.0.0.jar')
        assert file.exists()
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments((commands + ['--stacktrace']) as String[])
                .withPluginClasspath([new File('build/classes/main'), new File('build/resources/main'), file])
    }

    BuildResult run(String... commands) {
        BuildResult result = gradle(commands).build()
        // for debug
        println result.getStandardOutput()
        println result.getStandardError()
        return result
    }
}
