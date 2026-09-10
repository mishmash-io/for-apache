# Distributed computing stacks images

This directory contains build scripts for distributed computing stacks images.

Use these images to:
- **Run pre-built distributed computing services**
- **Extend your app images with distributed computing stacks**

Choose images based on your architecture - whether you're embedding distributed computing services within your app or running them separately.

> [!TIP]
>
> This document only covers machine images that contain individual distributed
> computing stacks. It does not cover the stacks themselves.
>
> To find out more about the stacks, [start with the main docs here.](../#readme)

## Stack vs Service images

Two types of images are available: stack images and service images.

Stack images contain only the components of a specific functionality, such as logging, JSON-serialization and deserialization, http server logic, etc. They are used at **build time** - only to install the functionality into another image. Once that functionality is installed, the image can be discarded. Stack images do not launch services and should not be used in **runtime** scenarios.

On the other hand, service images are intended to be used at **runtime** to
directly launch a given service. They are typically built from stack images and include the most common set of components needed to operate that service.

Stack images are typically named with the `stacks-` prefix, while service images just
follow the name of the service they provide.

## Getting pre-built images

We're building and publishing container images on [mishmash.io Docker Hub](https://hub.docker.com/u/mishmashio).

> [!IMPORTANT]
> 
> VM images will be published in the near future.

## Using service images



## List of service images

Use these images to directly launch a service.

- `quorum-server`
  
  Ready to use Quorum server, suitable for most deployment scenarios.

## Using stack images

For container images a stack is typically used by adding a stage to a container build:

```dockerfile
FROM mishmashio/<chosen-stack-image> AS stack
...
```

... and later copying its `/opt/stacks` directory, as it typically contains everything
needed:

```dockerfile
FROM my-base-image

...

COPY --from=stack /opt/stacks /opt/stacks

...

```

Deviations from this approach will be documented within the specific stack image's documentation.

## List of stacks images

Here's a list of the available stacks images and their contents:

- `stacks-jetty-base`

  Jars used by both [Eclipse Jetty](https://jetty.org/) clients and servers. Versions follow Jetty releases.

  Not intended for direct use, choose one (or more) of `stacks-jetty-client` and `stacks-jetty-server` instead.

- `stacks-jetty-client`
  
  The [Eclipse Jetty](https://jetty.org/) client libraries. Versions follow Jetty releases.

- `stacks-jetty-server`
  
  The [Eclipse Jetty](https://jetty.org/) server libraries. Versions follow Jetty releases.

- `stacks-jetty-servlet`
  
  Extends the [Eclipse Jetty](https://jetty.org/) server stack with servlet support. Versions follow Jetty releases.

  **Requires** `stacks-jetty-server` to also be installed.

- `stacks-gson`
  
  [Google Gson](https://github.com/google/gson) for JSON serialization and deserialization. Stack images have the same versions as Gson releases.

- `stacks-yaml`

  YAML support for configuration and data serialization. Its backed by
  [SnakeYAML](https://snakeyaml.com) and versioned according to its releases.

- `stacks-jackson`

  [Jackson](https://github.com/fasterxml/jackson) for JSON serialization and deserialization. Image versions are the same as Jackson releases.

  Can be extended with `stacks-jackson-yaml` when YAML support is needed.

- `stacks-jackson-yaml`
  
  Adds YAML support to the [Jackson](https://github.com/fasterxml/jackson) stack. Image versions are the same as Jackson releases.

  **Requires** `stacks-jackson` and `stacks-yaml` to work properly.

- `stacks-logging`
  
  Logging backend, more about it [here.](logging/#readme)

- `stacks-slf4j`
  
  [SLF4J logging facade,](https://www.slf4j.org) same versions as SLF4J releases.
  
  **Needs** the [logging backend stack.](logging/#readme)

- `stacks-netty`
  
  Base [Netty](https://netty.io) networking functionality, including native transport support for Linux. Image versions follow Netty releases.

- `stacks-opentelemetry-agent`
  
  Sets up the [OpenTelemetry Java agent.](https://opentelemetry.io/docs/zero-code/java/agent/) For configuration options - [read this.](opentelemetry-agent/#readme)

- `stacks-sasl-oidc`
  
  Provides a SASL login module with OIDC (OpenID Connect) authentication.
  Refer to [its own docs](sasl-oidc/#readme) for details.

- `stacks-bouncy-castle`
  
  The [Bouncy Castle](https://www.bouncycastle.org) cryptographic libraries. Follows Bouncy Castle versioning.

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

## Building images

Container images are built using [this multi-stage Dockerfile](Dockerfile).

For simplicity, you can also use the [build-container-images.sh script.](build-container-images.sh) It builds and correctly tags all container images
with their versions.

## How *stacking* works

In this document, *stacking* refers to the practice of building container images that include one or more distributed computing stacks. Each stack represents a specific functionality (all its dependencies and requirements), and by *stacking* images together, you can create a comprehensive environment for your distributed apps.

A typical stack image conatains:
- all necessary JARs
- *stacking* shell scripts
- service launch scripts (if the image contains a launchable service)
- optionally default configuration files, admin and utility command-line tools, etc.

> [!IMPORTANT]
>
> A stack may also require another stack to be present in the image. This will be
> noted in the stack's documentation.
>
> Ensure all necessary stacks are included in your builds.

Under the hood, it is the *stacking* shell scripts that do the necessary work so that
stacks *plug-in* properly. This is done by installing `bash` functions which are then
discovered by launch scripts and executed during specific stages of the launch.

Such `bash` functions typically add JAR files to the classpath, set Java system
properties and environment variables, or add new command-line options to the launch (or admin) scripts, and more.

This functionality is adopted from the [Apache Hadoop Unix Shell Guide](https://hadoop.apache.org/docs/r3.5.0/hadoop-project-dist/hadoop-common/UnixShellGuide.html) and its `shell profiles` mechanism.

For an example of a shell profile - see [this shell script.](quorum-server/shellprofile.d/quorum-server.sh) It adds a number of JARs to the classpath (later to be used by the quorum service launch script) and potentially checks an environment
variable to decide if OpenTelemetry instrumentation is disabled. If instrumentation 
is not disabled, the functions add a JVM command-line option to point the OpenTelemetry agent to its configuration file.

## Adding new stacks

1. In a maven pom - copy the necessary dependencies to the given path (typically under `/opt/stacks`)
2. Install a shell script in `libexec/shellprofile.d` and in it add the
necessary entries to the classpath, add additional JVM args, etc.
3. Add to the build files - `Dockerfile`, `build-container-images.sh`, etc
