# Logging stack image

This stack contains the [Apache Log4j 2](https://logging.apache.org/log4j/2.x/) **logging backend,** including its JUL (`java.util.logging`) bridge.

## Using the logging stack

If you follow the [general instructions for using stack images](../#readme), then using this stack is as easy as copying its `/opt/stacks` directory into your image (during a multi-stage build, for example):

```dockerfile
FROM stacks-logging:latest AS logging

# Add the logging stack
COPY --from=logging /opt/stacks /opt/stacks

...
```

All necessary jars and init scripts will be automatically *plugged in.* If you need to customize this automatic, *stacking* mechanism - check out the [distributed computing stacks documentation.](../#readme)

## Image versions

Image versions follow Log4j 2 release versions. For ease of use, a `latest`
tag is also available wherever possible.

For example, container images will be tagged with their corresponding Log4j 2
version AND the tag `latest`.

> [!WARNING]
>
> Container image tags are not immutable. That is, one tag may point to different
> images (and content) over time.
>
> We strongly recommend using using concrete hashes AND some dependency tracking
> mechanism that will notify you when we update the images.
>
> Doing both is the only way to be sure you are consistently deploying CVE-free
> software.

## Configuration

At runtime, you need to provide a Log4j 2 configuration file on the classpath.
The file must be in a supported format. This stack only supports the `.properties` format. Add the [json-jackson](../json-jackson/) stack to get `.json` config file format, and add [json-jackson](../json-jackson/) and [yaml-jackson](../yaml-jackson/) stacks to get `.yaml` as well.

The [Configuration section of the Apache Log4j 2 manual](https://logging.apache.org/log4j/2.x/manual/configuration.html) tells you where to place your configuration file
and also what to write in it.

### Debugging your configuration

Set the `DEBUG_LOG4J` environment variable before running and Log4j 2
will print debug information about its own operation. This can be useful for troubleshooting log configuration issues.

### JUL Integration

When installed, this stack automatically configures JUL (`java.util.logging`) to route its logs through the Log4j 2 backend. It sets the `java.util.logging.manager` system property to `org.apache.logging.log4j.jul.LogManager`.

## Using with logging facades

If you need the SLF4j facade deploy [its stack](../logging-slf4j/) alongside
this stack. It will be automatically *plugged in.*

---

## About the distributed computing stacks

For a broader view on the distributed computing stacks start at the [main documentation here.](../../#readme) Also [see this document](../#readme) specifically
about the stacks images.
