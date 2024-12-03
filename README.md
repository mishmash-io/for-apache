# Open Source Stacks for Distributed Computing, by mishmash.io

This repository contains [mishmash.io](https://mishmash.io) builds of open source
projects and frameworks that are popular in distributed computing.

At mishmash.io, we use a lot of open source - in our [distributed database](https://mishmash.io/docs/database) or other software that we publish (such as our [open source analytics for OpenTelemetry](https://github.com/mishmash-io/opentelemetry-server-embedded)). Often we patch and adapt open source code to better suit our needs and we're publishing such customizations as **stacks** here.

## Why do we rebuild other open source projects?

> [!NOTE]
> Three major reasons why we customize and rebuild other open source projects:
> - Publishing secure software
>   
>   We update code and dependencies to latest versions, especially when new
>   vulnerabilities are reported and **fixes are published.**   
> - Unfied set of dependencies
>   
>   We modify open source projects to use the same set of dependencies (and their versions).
> - Minimal software packages
>   
>   We break down larger open source projects with multiple features into
>   smaller, 'per-feature' modules that can be used **on an as-needed basis.**

For example, our distributed database **mishmash io** uses some core functionalities from [Apache HDFS](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html), namely, to manage cold- or hot-storage disks; to replicate large data blocks across zones and clusters; and to provide **zero-copy** data access. Apache HDFS is part of [Apache Hadoop](https://hadoop.apache.org/) and comes with many more features (such as REST-based management APIs or Web GUI apps) and these extra features come with their additional code and dependencies.

By splitting HDFS into smaller, feature-based modules we **minimize image sizes** 
of software we publish and simultaneously **reduce the attack surface.**

Also, as we officially support all software that we deliver, including its dependencies - we would like to make our lives easier by supporting as little code
as possible. Therefore, we modify open source projects to use a single dependency for logging; another single dependency for networking; and so on.

## How we do it?

(Coming soon)

## The Stacks

(Coming soon)
