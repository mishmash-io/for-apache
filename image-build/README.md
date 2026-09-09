# Images for the distributed computing stacks

This directory contains build scripts for distributed computing stacks images.

Use these images to:
- **Run pre-built distributed computing services**
- **Extend your app images with distributed computing stacks**

Choose images based on your architecture - whether you're embedding distributed computing services within your app or running them separately.

## Getting pre-built images

We're building and publishing container images on [mishmash.io Docker Hub](https://hub.docker.com/u/mishmashio).

VM images will be published in the near future.

## List of service images

Use these images to directly launch a service.

- `quorum-server`
  
  Ready to use Quorum server, suitable for most deployment scenarios.

## List of stacks images

Here's a list of the available stacks images and their contents:

- `stacks-jetty-base`

  Jars used by both jetty clients and servers.

- `stacks-jetty-client`
  
  The jetty client libraries.

- `stacks-jetty-server`
  
  The jetty server libraries.

- `stacks-jetty-servlet`
  
  Extends the jetty server stack with servlet support.

- `stacks-gson`
  
  Google Gson for JSON serialization and deserialization.

- `stacks-yaml`

  YAML support for configuration and data serialization.

- `stacks-jackson`

  Jackson for JSON serialization and deserialization.

- `stacks-jackson-yaml`
  
  Adds YAML support to the Jackson stack.

- `stacks-logging`
  
  Logging backend, more about it [here.](logging/README.md)

- `stacks-slf4j`
  
  SLF4J logging facade. Needs the [logging backend stack.](logging/README.md)

- `stacks-netty`
  
  Base Netty networking functionality.

- `stacks-opentelemetry-agent`
  
  Sets up the OpenTelemetry Java agent.

- `stacks-sasl-oidc`
  
  Provides a SASL module for OIDC (OpenID Connect) authentication.

- `stacks-bouncy-castle`
  
  The bouncy castle cryptographic libraries.

- `shellprofiles-container`
  
  Utility scripts when running stacks in containerized environments.

- `stacks-quorum-client`

  The Quorum client stack.

- `stacks-quorum-server`
  
  The Quorum server stack.

- `stacks-quorum-admin-rest`
  
  Extends a Quorum server with administrative REST services.

- `stacks-quorum-controller`
  
  Adds a HTTP controller module to a Quorum server.

## Using stack images

```dockerfile
FROM eclipse-temurin:25-jre-noble:latest

...
```

## Building images

## Adding new stacks

1. In a maven pom - copy the necessary dependencies to the given path (typically under `/opt/stacks`)
2. Install a shell script in `libexec/shellprofile.d` and in it add the
necessary entries to the classpath, add additional JVM args, etc.
3. Add to the build files - `Dockerfile`, `build-container-images.sh`, etc

[Apache Hadoop Unix Shell Guide](https://hadoop.apache.org/docs/r3.5.0/hadoop-project-dist/hadoop-common/UnixShellGuide.html)
