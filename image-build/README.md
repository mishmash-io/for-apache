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

  Jars used by both [Eclipse Jetty](https://jetty.org/) clients and servers.

- `stacks-jetty-client`
  
  The [Eclipse Jetty](https://jetty.org/) client libraries.

- `stacks-jetty-server`
  
  The [Eclipse Jetty](https://jetty.org/) server libraries.

- `stacks-jetty-servlet`
  
  Extends the [Eclipse Jetty](https://jetty.org/) server stack with servlet support.

- `stacks-gson`
  
  [Google Gson](https://github.com/google/gson) for JSON serialization and deserialization.

- `stacks-yaml`

  YAML support for configuration and data serialization.

- `stacks-jackson`

  [Jackson](https://github.com/fasterxml/jackson) for JSON serialization and deserialization.

- `stacks-jackson-yaml`
  
  Adds YAML support to the [Jackson](https://github.com/fasterxml/jackson) stack.

- `stacks-logging`
  
  Logging backend, more about it [here.](logging/#readme)

- `stacks-slf4j`
  
  [SLF4J logging facade.](https://www.slf4j.org) Needs the [logging backend stack.](logging/#readme)

- `stacks-netty`
  
  Base [Netty](https://netty.io) networking functionality, including native transport support.

- `stacks-opentelemetry-agent`
  
  Sets up the [OpenTelemetry Java agent.](https://opentelemetry.io/docs/zero-code/java/agent/) For configuration options - [read this.](opentelemetry-agent/#readme)

- `stacks-sasl-oidc`
  
  Provides a SASL login module with OIDC (OpenID Connect) authentication.
  Refer to [its own docs](sasl-oidc/#readme) for details.

- `stacks-bouncy-castle`
  
  The [Bouncy Castle](https://www.bouncycastle.org) cryptographic libraries.

- `shellprofiles-container`
  
  Utility scripts when running stacks in containerized environments. [Learn more here](shellprofiles-container/#readme)

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
