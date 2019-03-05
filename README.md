# Integration Testing with Containers

This project conatins a basic Spring Boot app with  Spring Boot Actuator and Spring JPA with backing from MySQL. The goal of this
project is to provide an example of how to integration test with containers.

### Requirements

* JDK 1.8 
* Maven 
* Docker  

### Running 

```
mvn clean verify
```
This *should*:
1. Compile the application.
2. Recompile using the Spring Boot Maven plugin.
3. Build a Docker container with the jar.
4. Run maven-failsafe with tests matching IT(Integration Test).


### Resources 
| Resource        | URL          | 
| ------------- |:-------------:|
https://github.com/spotify/docker-maven-plugin |Builds an docker image with the Spring Boot app.
https://www.testcontainers.org |Runs a container(s) during the integration test phase.
https://docs.spring.io/spring-boot/docs/current/maven-plugin/usage.html |Repackages the compiled jar to make it Spring Boot executable.
https://maven.apache.org/surefire/maven-failsafe-plugin/ | Maven plugin designed to run Integration tests.


### Reading Materials:
* https://docs.docker.com/config/containers/container-networking/
* https://spring.io/guides/gs/accessing-data-jpa/

