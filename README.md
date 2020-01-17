# Gradle Java-lib plugin
[![License](https://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/travis/xvik/gradle-java-lib-plugin.svg)](https://travis-ci.org/xvik/gradle-java-lib-plugin)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/gradle-java-lib-plugin?svg=true)](https://ci.appveyor.com/project/xvik/gradle-java-lib-plugin)
[![codecov](https://codecov.io/gh/xvik/gradle-java-lib-plugin/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/gradle-java-lib-plugin)


### About

Plugin mainly removes boilerplate for common java or groovy library configuration (and gradle plugin).
Configures publication ([maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) 
) with all artifacts, required for maven central publishing (central requires 3 artifacts with correct pom).

Features:

* Maven-like `jar` configuration
    - put `pom.xml` and `pom.properties` inside jar
    - fill manifest properties 
* Configure tasks for additional artifacts (required for maven central publish): 
    - `sourcesJar`  
    - `javadocJar` or (and) `groovydocJar`
* Prepare maven publication (`maven-publish`)
    - `maven` publication configured with all jars (jar, sources javadock or (and) groovydoc)
    - for gradle plugin projects, configure existing `pluginMaven` publication
* Applies [pom plugin](https://github.com/xvik/gradle-pom-plugin) which:   
    - Fix [dependencies scopes](https://github.com/xvik/gradle-pom-plugin/#dependencies) 
    in generated pom
    - Add `pom` configuration closure to [simplify pom definition](https://github.com/xvik/gradle-pom-plugin#pom-configuration).
    - Add `withPomXml` configuration closure to use if you [need manual xml configuration](https://github.com/xvik/gradle-pom-plugin#manual-pom-modification) 
* Add `install` task as shortcut for `publishToMavenLocal`
* Apply `UTF-8` encoding for:
    - compile tasks: `JavaCompile` and `GroovyCompile`
    - javadoc (encoding, charSet, docencoding) 
    - test tasls: set `file.encoding=UTF-8` system property (only for test tasks)  

If your project is hosted on github you may look to [github-info plugin](https://github.com/xvik/gradle-github-info-plugin) 
which fills some pom sections for you automatically. 

If you need [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) from the same project, 
then you will have to perform additional configuration or, maybe (depends on case), use only [pom plugin](https://github.com/xvik/gradle-pom-plugin). 

**Confusion point**: plugin named almost the same as gradle's own [java-library](https://docs.gradle.org/current/userguide/java_library_plugin.html) plugin,
but plugins do *different things* (gradle plugin only provides `api` and `implementation` configurations) and plugins *could* be used together.

##### Summary

* Configuration closures: `pom`, `withPomXml`
* Tasks: `sourcesJar`, `javadocJar` (`groovydocJar`), `install`      
* [Publication](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publications): `maven` or `pluginMaven`
* Enable plugins: [maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html),
[ru.vyarus.pom](https://github.com/xvik/gradle-pom-plugin)


### Setup

Releases are published to [bintray jcenter](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/), 
[maven central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin) and 
[gradle plugins portal](https://plugins.gradle.org/plugin/ru.vyarus.java-lib).

[![JCenter](https://api.bintray.com/packages/vyarus/xvik/gradle-java-lib-plugin/images/download.svg)](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/gradle-java-lib-plugin.svg)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/gradle-java-lib-plugin)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:2.0.0'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

OR

```groovy
plugins {
    id 'ru.vyarus.java-lib' version '2.0.0'
}
```

#### Compatibility

Plugin compiled for java 8, compatible with java 11

Gradle | Version
--------|-------
5.1     | 2.0.0
4.6     | [1.1.2](https://github.com/xvik/gradle-java-lib-plugin/tree/1.1.2)
older   | [1.0.5](https://github.com/xvik/gradle-java-lib-plugin/tree/1.0.5)

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>

* Go to [JitPack project page](https://jitpack.io/#xvik/gradle-java-lib-plugin)
* Select `Commits` section and click `Get it` on commit you want to use (you may need to wait while version builds if no one requested it before)
    or use `master-SNAPSHOT` to use the most recent snapshot

For gradle before 6.0 use `buildscript` block with required commit hash as version:

```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath 'com.github.xvik:gradle-java-lib-plugin:b5a8aee24f'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

For gradle 6.0 and above:

* Add to `settings.gradle` (top most!) with required commit hash as version:

  ```groovy
  pluginManagement {
      resolutionStrategy {
          eachPlugin {
              if (requested.id.namespace == 'ru.vyarus.java-lib') {
                  useModule('com.github.xvik:gradle-java-lib-plugin:b5a8aee24f')
              }
          }
      }
      repositories {
          maven { url 'https://jitpack.io' }
          gradlePluginPortal()          
      }
  }    
  ``` 
* Use plugin without declaring version: 

  ```groovy
  plugins {
      id 'ru.vyarus.java-lib'
  }
  ```  

</details>  

### Usage

#### General

You need to specify general info:

```groovy
group = 'your.group'                    // maven group
version = '1.0.0'                       // project version
description = 'My project description'  // optional (affects jar manifest) 
```

Note: maven `artifactId` will be the same as project name and the default for project name
is current directory name. If you need to change name, add in `settings.gradle`:

```
rootProject.name = 'the-name-you-want'
```        

#### Encodings

UTF-8 applied to:

* (all `CompileJava` tasks).options.encoding 
* (all `CompileGrovy` tasks).options.encoding
* (all `Javadoc`).options.\[encoding, charSet, docEncoding]
* (all `Test`).systemProperty 'file.encoding'

Note that groovydoc task does not have encoding configuration, but it should use UTF-8 by defautl. 

For tests, encoding is important (especially on windows) because test forked process will not inherit root gradle encoding configuration. 

#### Tasks 

- `sourcesJar` task is always applied 
- `javadocJar` if java sources directory present ('src/main/java')
- `groovydocJar` if groovy plugin available and groovy sources present ('src/main/groovy'). Last condition is important because groovy may be used only for tests.

IMPORTANT: if you have only groovy sources then `groovydocJar` will have javadoc` classifier! This is because maven central requires
javadoc jar, so even if you write groovy project you have to name it javadoc.

In case of both groovy and java sources, `groovydocJar` will use `groovydoc` classifier, because `javadocJar` already use `javadoc` and have to produce separate artifacts.   

`install` task added to simplify publication to local maven repository: this is simply shortcut for
gradle's [publishToMavenLocal](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:tasks) task
(simply shorter to type and more common name after maven).  

#### Pom

Most likely, you will need to configure pom:

```groovy
pom {
    name 'Project Name'
    description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }
    developers {
        developer {
            id "dev1"
            name "Dev1 Name"
            email "dev1@email.com"
        }
    }
}
```

Read more about pom specifics in [pom plugin description](https://github.com/xvik/gradle-pom-plugin).

Use the following configuration to get correct scope in the resulted pom:

Maven scope | Gradle configuration
------------| ----------------
compile     | implementation, api
runtime     | runtimeOnly
provided    | compileOnly
optional    | [gradle feature](#optional-dependencies)

#### Main Jar

Plugin applies default manifest properties:

```groovy
'Implementation-Title': project.description ?: project.name,
'Implementation-Version': project.version,
'Built-By': System.getProperty('user.name'),
'Built-Date': new Date(),
'Built-JDK': System.getProperty('java.version'),
'Built-Gradle': gradle.gradleVersion,
'Target-JDK': project.targetCompatibility
```

You can override it:

```groovy
jar {
    manifest {
        attributes 'Implementation-Title': 'My Custom value',
            'Built-By': 'Me'
    }
}
```

For all not specified properties default values will be used.

Plugin will include additional files inside jar (like maven do) into `META-INF/maven/group/artifact/`

* pom.xml
* pom.properties

pom.properties contains:

* version
* groupId
* artifactId

#### Publication

[maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin is used for publication.

By default, plugin configures `maven` publication with javadoc or (and) groovydoc and sources jars. 

Use `install` task to deploy jars into local maven repository.

```bash
$ gradlew install
``` 

If you don't want to publish everything (jar, sources, javadoc) then you can override list of publishing artifacts:

```groovy
publishing.publications.maven.artifacts = [jar, javadocJar]
```

Here sources are excluded from publishing (note that if you going to publish to maven central sources are required).

##### Gradle plugin

Gradle plugin project will have `java-gradle-plugin`, which declares it's own maven publication `pluginMaven`.
It is not possible to re-configure it (name hardcoded) so java-lib plugin will not create `maven` publication and
will configure existing `pluginMaven` instead.

This way, only one publication would be prepared for gradle plugin. The same publication will be used by
`plugin-publish` plugin (to publish into plugins portal) and by `maven-publish` (if you will need to also publish to other repositories)  

Note that, by default `java-gradle-plugin` creates it's own sources and javadoc tasks, but `java-lib` plugin will
disable this default behavior, so only `souresJar` and `javadocJar` (`groovydocJar`) tasks created by plugin would be used.

##### Publish to repository 
 
For actual publication only [repository](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:repositories) must be configured:

```groovy
publishing {
    repositories {
        maven {
            // change to point to your repo, e.g. http://my.org/repo
            url "$buildDir/repo"
        }
    }
}
```

Then [publish task](https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:publishing) may be used to perform publish. 

##### Publish to bintray
 
If you publish to [bintray](https://bintray.com/), then you need to reference `maven` publication 
in [bintray plugin](https://github.com/bintray/gradle-bintray-plugin) configuration:
 
```groovy
bintray {    
    publications = ['maven']
    ...
}    
```

For gradle plugin use `pluginMaven` publication name.

### Complete usage example

```groovy
plugins {
    id 'java'  // or groovy or java-library
    id 'ru.vyarus.java-lib' version '2.0.0'
    id 'com.jfrog.bintray' version '1.7.1'
}

group = 'com.sample' 
version = '1.0.0'
description = 'Sample project'

sourceCompatibility = 1.8

repositories { jcenter() }
dependencies {    
    compileOnly 'com.foo:dep-provided:1.0'
    implementation 'com.foo:dep-compile:1.0'        
}

pom {
    name 'Project Name'
    description 'My awesome project'
    licenses {
        license {
            name "The MIT License"
            url "http://www.opensource.org/licenses/MIT"
            distribution 'repo'
        }
    }
    scm {
        url 'https://github.com/me/my-repo.git'
        connection 'scm:git@github.com:me/my-repo.git'
        developerConnection 'scm:git@github.com:me/my-repo.git'
    }
    developers {
        developer {
            id "dev1"
            name "Dev1 Name"
            email "dev1@email.com"
        }
    }
}

bintray {    
    publications = ['maven']
    ...
}  
```

### Boilerplate plugin removes

Just for reference, here is what plugin did for java project:

```groovy
apply plugin: 'ru.vyarus.pom'

jar {    
    manifest {
        attributes 'Implementation-Title': project.description ?: project.name,
                'Implementation-Version': project.version,
                'Built-By': System.getProperty('user.name'),
                'Built-Date': new Date(),
                'Built-JDK': System.getProperty('java.version'),
                'Built-Gradle': gradle.gradleVersion,
                'Target-JDK': project.targetCompatibility
    }
}

task generatePomPropertiesFile {
    inputs.properties ([
            'version': "${ -> project.version }",
            'groupId': "${ -> project.group }",
            'artifactId': "${ -> project.name }"
    ])
    outputs.file "$project.buildDir/generatePomPropertiesFile/pom.properties"
    doLast {
        File file = outputs.files.singleFile
        file.parentFile.mkdirs()
        file << inputs.properties.collect{ key, value -> "$key: $value" }.join('\n')
    }
}

model {
    tasks.jar {
        into("META-INF/maven/$project.group/$project.name") {
            from generatePomFileForMavenPublication
            rename ".*.xml", "pom.xml"
            from generatePomPropertiesFile
        }
    }
}

task sourcesJar(type: Jar, dependsOn: classes, group: 'build') {
	from sourceSets.main.allSource
	classifier = 'sources'
}

task javadocJar(type: Jar, dependsOn: javadoc, group: 'build') {
	classifier 'javadoc'
    from javadoc.destinationDir
}

task groovydocJar(type: Jar, dependsOn: groovydoc, group: 'build') {
	classifier 'groovydoc'
    from groovydoc.destinationDir 
    options.encoding = StandardCharsets.UTF_8
    options.charSet = StandardCharsets.UTF_8
    options.docEncoding = StandardCharsets.UTF_8
}    

tasks.withType(JavaCompile).configureEach {
    it.options.encoding = StandardCharsets.UTF_8
}

tasks.withType(GroovyCompile).configureEach {
    it.options.encoding = StandardCharsets.UTF_8
}

tasks.withType(Test).configureEach {
   it.systemProperty JvmOptions.FILE_ENCODING_KEY, StandardCharsets.UTF_8
}

publishing {
	publications {
	    maven(MavenPublication) {
	        from components.java
	        artifact sourcesJar	        
	        artifact javadocJar
	        artifact groovydocJar	 
	    }
	}
}

task install(dependsOn: publishToMavenLocal, group: 'publishing') << {
	logger.warn "INSTALLED $project.group:$project.name:$project.version"
}
```

### Might also like

* [quality-plugin](https://github.com/xvik/gradle-quality-plugin) - java and groovy source quality checks
* [github-info-plugin](https://github.com/xvik/gradle-github-info-plugin) - pre-configure common plugins with github related info
* [animalsniffer-plugin](https://github.com/xvik/gradle-animalsniffer-plugin) - java compatibility checks
* [mkdocs-plugin](https://github.com/xvik/gradle-mkdocs-plugin) - project documentation generator 
* [java-library generator](https://github.com/xvik/generator-lib-java) - java library project generator

---
[![gradle plugin generator](http://img.shields.io/badge/Powered%20by-%20Gradle%20plugin%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-gradle-plugin)
