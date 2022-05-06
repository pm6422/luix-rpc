# LUI️✘

LUI️✘ is pronounced [ˈluːɪks], which is a RPC(Remote Procedure Call) and service governance framework, it can make calls between services more simple.

## Official Website
Please visit our website for more details.

[http://www.luixtech.com](http://www.luixtech.com)


## Requirements

Before you can build this project, you must install JDK on your machine:

You can install JDK by [SDK Man](https://sdkman.io/install). e.g.

```
sdk install java 8.0.312-zulu
```

Java compatability: JDK 8 ~ 17

Docker is optional, unit testing programs can start dockerized mongo, consul instance.

[Docker Installation Docs](https://docs.docker.com/engine/install)

## Build

Run the following command to build the project:

```
./mvnw clean verify
```

### Building for production

To build the final jar for production, run:

```
./mvnw -Pprod clean verify
```

## Run

Run Luix Demo Server
```
java -jar luix-demo/luix-demo-server/target/*.jar
```

Then navigate to [http://localhost:6010](http://localhost:6010) in your browser.

Run Luix Demo Client

```
java -jar luix-demo/luix-demo-client/target/*.jar
```

Then navigate to [http://localhost:6020](http://localhost:6020) in your browser.

Run Luix Web Center

```
java -jar luix-web-center/target/*.jar
```

Then navigate to [http://localhost:6030](http://localhost:6030) in your browser.

## Testing

To launch your application's tests, run:

```
./mvnw verify
```

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker-compose -f docker/sonar.yml up -d
```

Note: we have turned off authentication in [docker/sonar.yml](docker/sonar.yml) for out of the box experience while trying out SonarQube, for real use cases turn it back on.

You can run a Sonar analysis with using the [sonar-scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner) or by using the maven plugin.

Then, run a Sonar analysis:

```
./mvnw -Pprod clean verify sonar:sonar
```

If you need to re-run the Sonar phase, please be sure to specify at least the `initialize` phase since Sonar properties are loaded from the sonar-project.properties file.

```
./mvnw initialize sonar:sonar
```

For more information, refer to the [Code quality page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your LUI️✘ development experience. A number of docker-compose configuration are available in the [docker](docker) folder to launch required third party services.

For example, to start a mongodb database in a docker container, run:

```
docker-compose -f docker/mongodb.yml up -d
```

To stop it and remove the container, run:

```
docker-compose -f docker/mongodb.yml down
```

To start local Consul registry, run:
```
docker-compose -f docker/consul.yml up -d
```

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build docker images of all your applications then push them to docker registry:

```
./mvnw package -DskipTests -DskipJibBuild=false
```

Then run:

```
docker-compose -f docker/app.yml up -d
```

To start local Prometheus and Grafana services, run:
```
docker-compose -f docker/monitoring.yml up -d
```
Then navigate to http://localhost:3000 in your browser to access Grafana with username: admin and password: admin

[jhipster homepage and latest documentation]: https://www.jhipster.tech
[jhipster 7.4.0 archive]: https://www.jhipster.tech/documentation-archive/v7.4.0
[using jhipster in development]: https://www.jhipster.tech/documentation-archive/v7.4.0/development/
[using docker and docker-compose]: https://www.jhipster.tech/documentation-archive/v7.4.0/docker-compose
[using jhipster in production]: https://www.jhipster.tech/documentation-archive/v7.4.0/production/
[running tests page]: https://www.jhipster.tech/documentation-archive/v7.4.0/running-tests/
[code quality page]: https://www.jhipster.tech/documentation-archive/v7.4.0/code-quality/
[setting up continuous integration]: https://www.jhipster.tech/documentation-archive/v7.4.0/setting-up-ci/
[node.js]: https://nodejs.org/
[npm]: https://www.npmjs.com/
[webpack]: https://webpack.github.io/
[browsersync]: https://www.browsersync.io/
[jest]: https://facebook.github.io/jest/
[leaflet]: https://leafletjs.com/
[definitelytyped]: https://definitelytyped.org/
[angular cli]: https://cli.angular.io/
