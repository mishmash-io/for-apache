# Logging Stack Image

This stack contains the log4j2 'logging backend' libraries, including
the JUL (`java.util.logging`) bridge.

## Using the Logging Stack

In a multi-stage container build, add it like this:

```dockerfile
FROM stacks-logging:latest AS logging

# Add the logging stack
COPY --from=logging /opt/stacks /opt/stacks

...
```

## Configuration

At runtime, you need to provide a log4j2 configuration file on the classpath.
The file must be in a supported format. This stack only supports the `.properties` format. Add the [json-jackson](../json-jackson/) stack to get `.json` config file format, and add [json-jackson](../json-jackson/) and [yaml-jackson](../yaml-jackson/) stacks to get `.yaml` as well.

### Debugging your configuration

You can also set the `DEBUG_LOG4J` environment variable to debug the log4j2
backend itself. This can be useful for troubleshooting log configuration issues.

### JUL Integration

When installed, this stack automatically configures JUL (`java.util.logging`) to route its logs through the log4j2 backend - it sets the `java.util.logging.manager` system property to `org.apache.logging.log4j.jul.LogManager`.

## Using with logging facades

If you need the SLF4j facade deploy [its stack](../logging-slf4j/) alongside
this stack.
