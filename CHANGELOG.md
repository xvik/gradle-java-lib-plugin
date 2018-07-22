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