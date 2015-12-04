#Gradle Java-lib plugin
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/gradle-java-lib-plugin.svg)](https://travis-ci.org/xvik/gradle-java-lib-plugin)

### About

Plugin applies common configuration for java or groovy library: 

* Configure `jar` with default manifest and pom inside jar (like maven do)
* Add `sourcesJar` task
* Add `javadocJar` or (and) `groovydocJar` tasks
* Configures maven publication named `maven` with all jars (jar, sources javadock or (and) groovydoc)
* Add `install` task as shortcut for publishToMavenLocal
* Applies [pom plugin](https://github.com/xvik/gradle-pom-plugin) plugin which: 
  - Adds `optional` and `provided` configurations (affect only resulted pom)
  - Fix dependencies scopes in generated pom (from default runtime)
  - Add `pom` configuration closure to avoid maven-publish's withXml.

If you need [multiple publications](https://docs.gradle.org/current/userguide/publishing_maven.html#N17EB8) from the same project, 
then you will have to perform additional configuration or, maybe (depends on case), use only [pom plugin](https://github.com/xvik/gradle-pom-plugin). 

### Setup

Releases are published to [bintray jcenter](https://bintray.com/bintray/jcenter) and [gradle plugins portal](https://plugins.gradle.org).

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/gradle-java-lib-plugin.svg?label=jcenter)](https://bintray.com/vyarus/xvik/gradle-java-lib-plugin/_latestVersion)

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'ru.vyarus:gradle-java-lib-plugin:1.0.1'
    }
}
apply plugin: 'ru.vyarus.java-lib'
```

OR (waiting for approve)

```groovy
plugins {
    id 'ru.vyarus.java-lib' version '1.0.1'
}
```

Plugin must be applied after java or groovy plugins. Otherwise it will do nothing.

### Usage

#### General

You need to specify general info:

```groovy
group = 'your.group'                    // maven group
version = '1.0.0'                       // project version
description = 'My project description'  // optional (affects jar manifest) 
```

You may use optional and provided dependencies:

```groovy
dependencies {    
    provided 'com.foo:dep-provided:1.0'
    optional 'com.foo:dep-optional:1.0'        
}
```

#### Tasks 

- `sourcesJar` task is always applied 
- `javadocJar` if java sources directory present ('src/main/java')
- `groovydocJar` if groovy plugin available and groovy sources present ('src/main/groovy'). Last condition is important because groovy may be used only for tests.

IMPORTANT: if you have only groovy sources then `groovydocJar` will have javadoc` classifier! This is because maven central requires
javadoc jar, so even if you write groovy project you have to name it javadoc.

In case of both groovy and java sources, `groovydocJar` will use `groovydoc` classifier, because `javadocJar` already use `javadoc` and have to produce separate artifacts. 

#### Pom

Most likely, you will need to configure pom:

```groovy
pom {
    name 'Project Name'
    description 'My awesome project
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
            'Built-By': 'Me',
    }
}
```

For all not specified properties default values will be used.

Also, plugin will include pom file inside jar (like maven do): META-INF/maven/group/artifact/pom.xml.

#### Publication

[maven-publish](https://docs.gradle.org/current/userguide/publishing_maven.html) plugin is used for publication.

By default plugin configures `maven` publication with javadoc or (and) groovydoc and sources jars. 

Use 'install' task to deploy jars into local maven repository.

```bash
$ gradlew install
``` 

#### Publish to repository 
 
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

#### Publish to bintray
 
If you publish to [bintray](https://bintray.com/), then you need to reference `maven` publication 
in [bintray plugin](https://github.com/bintray/gradle-bintray-plugin) configuration:
 
```groovy
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
            'version': "${->project.version}",
            'groupId': "${->project.group}",
            'artifactId': "${->project.name}"
    ])
    outputs.file "$project.buildDir/generatePomPropertiesFile/pom.properties"
    doLast {
        File file = outputs.files.singleFile
        file.parentFile.mkdirs()
        file << inputs.properties.collect{key, value -> "$key: $value"}.join('\n')
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

publishing {
	publications {
	    maven(MavenPublication) {
	        from components.java
	        artifact sourcesJar	        
	        artifact javadocJar	        
	    }
	}
}

task install(dependsOn: publishToMavenLocal, group: 'publishing') << {
	logger.warn "INSTALLED $project.group:$project.name:$project.version"
}
```

### Complete usage example

```groovy
plugins {
    id 'java'
    id 'ru.vyarus.java-lib' version '1.0.0'
    id 'com.jfrog.bintray' version '1.4'
}

group = 'com.sample' 
version = '1.0.0'
description = 'Sample project'

sourceCompatibility = 1.6
targetCompatibility = 1.6

repositories { jcenter() }
dependencies {    
    provided 'com.foo:dep-provided:1.0'
    compile 'com.foo:dep-compile:1.0'        
}

pom {
    name 'Project Name'
    description 'My awesome project
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
