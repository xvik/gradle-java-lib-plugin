* Add disable publication option: javaLib.withoutPublication() (disables all declared publications)
  Useful for disabling bom publication in the root project (use platform for deps management only) 

### 2.2.1 (2021-07-13)
* Fix multi-module projects configuration with allprojects closure 
  (configuration extension always created on plugin activation to avoid mis-references)

### 2.2.0 (2021-06-22)
* Updated pom plugin (2.2.0): java-platform compatibility
* Add java-platform plugin support (assuming declared platform published as BOM):
    - activates platform dependencies (javaPlatform.allowDependencies())
    - register pom plugin 
    - add install task
    - registers "bom" publication (intentionally different name)
    - configures signing if signing plugin enabled
    - allows overriding default artifact name with javaLib.bom  configuration 
      (by default it's a root project name)
* Add automatic signing configuration when 'signing' plugin applied
  (for snapshots signing not required -  for release, not configured signing would fail)
* Add openDependencyReport task when project-report plugin enabled
  (task opens htmlDependencyReport directly in the browser)
* Enable jacoco xml report by default (required for coverage services)  
* Multi-module projects support: test and coverage reports aggregation
  (at least "base" plugin must be applied to trigger minimal java-lib plugin activation )
* Add `javaLib` configuration closure:
    - withoutGradleMetadata() - disables gradle metadata publishing
    - withoutJavadoc() and withoutSources() - disable javadoc and sources publish
    - bom.artifactId and bom.description properties - updates artifact declared with java-platform
    - pom - shortcut for the new pom plugin configuration closure (to use instead of pomGeneration)
    - autoModuleName - shortcut for defining Automatic-Module-Name manifest property
    - aggregateReports() - supposed to be used in the root project to aggregate
       test reports and jacoco coverage (adds test and jacocoTestReport tasks)
       Also, aggregates dependency report id project-report plugin enabled

### 2.1.0 (2020-01-19)
* Updated pom plugin (2.1.0): 
    - Brings back `provided` and `optional` scopes, because gradle native features can't completely replace them
    - `compileOnly` dependencies no more added to pom (default behaviour reverted)  

Versions 2.0.0 and 2.0.1 are not recommended for usage because of referenced pom plugin
(there was an attempt to replace optional and provided configurations with native gradle features, 
but it failed: custom configurations still required).

### 2.0.1 (2020-01-19) DON'T USE
* Updated pom plugin (2.0.1) containing fix for provided dependencies declared with BOM
* Revert to old behaviour: in case of gradle plugin project use "maven" publication because its not possible 
    to differentiate gralde plugin from usual project. In any case, artifacts will be exactly the same everywhere
    (plugin-publish javadoc and sources tasks will be disabled).  

### 2.0.0 (2020-01-17) DON'T USE
* (breaking) Requires gradle 5.1 and above
    - Remove legacy (lazy, without strict publishing) publication configuration 
* (breaking) Drop java 7 support
* (breaking) Updated pom plugin ([2.0.0](https://github.com/xvik/gradle-pom-plugin/releases/tag/2.0.0)) removes provided and optional scopes
    - provided scope replaced with standard compileOnly configuration support 
* Use gradle configuration avoidance to prevent not used tasks creation
* Set UTF-8 encoding for JavaCompile and GroovyCompile tasks
* Set file.encoding=UTF-8 system property for Test tasks
* Set UTF-8 encoding for javadoc task (encoding, charSet, docencoding) 
* Gradle plugin projects compatibility fixes:
    - When used with java-gardle-plugin, re-use pluginMaven publication instead of creating
        new one (because java-gardle-plugin hardcode publication name and it has to init it because of alias publications )
    - Plugin-publish will not create his own javadoc and sources tasks (so java-lib tasks will be used)

### 1.1.2 (2018-07-22)
* Fix missed pom dependencies
* Unify stable/lazy behaviours

### 1.1.1 (2018-07-13)
* Fix stable publishing detection

### 1.1.0 (2018-07-13)
* Gradle 4.8 [STABLE_PUBLISHING](https://docs.gradle.org/4.8/userguide/publishing_maven.html#publishing_maven:deferred_configuration) support
    - Plugin requires gradle 4.6 or above (will fail on earlier gradle).
    - Gradle 4.6, 4.7 - legacy mode (as before)
    - (breaking) Gradle 4.8, <5.0 - updated pom plugin (1.3.0) will automatically enable STABLE_PUBLISHING mode         
    - Gradle 5.0 and above - assumed stable publishing enabled by default

Important: when STABLE_PUBLISHING is enabled (gradle 4.8 and above) publishing configurations will NOT work 
in a lazy way as before. Use `afterEvaluate {}` INSIDE publication configuration in order to configure lazy properties               

### 1.0.5 (2017-08-15)
* Update pom plugin (support gradle java-library plugin)

### 1.0.4 (2016-09-05)
* Update pom plugin (fix non string values support, add clashing tag names workaround, add manual xml configuration shortcut)

### 1.0.3 (2016-07-29)
* Update pom plugin (fix pom closure merge for repeatable tags like developers)

### 1.0.2 (2016-05-20)
* Update pom plugin (fix pom dependencies scopes update)

### 1.0.1 (2015-12-05)
* groovydocJar use javadoc classifier if no java sources available (because maven central requires javadoc for publication)
* Generate pom.properties inside jar's META-INF (like maven)

### 1.0.0 (2015-11-23)
* Initial release