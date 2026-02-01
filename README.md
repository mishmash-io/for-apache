# Open Source Stacks for Distributed Computing

[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/mishmash-io/distributed-computing-stacks/badge)](https://scorecard.dev/viewer/?uri=github.com/mishmash-io/distributed-computing-stacks)


#### In this repository you'll find ***derivative works*** of other open source projects that are popular for building distributed apps and clusters.

With a few exceptions, the code built here is originally developed by other parties (find the list below) and then customized by [mishmash.io](https://mishmash.io).

At mishmash.io, we use a lot of open source - in our [distributed database](https://mishmash.io/docs/database) or other software that we publish (such as our [open source analytics for OpenTelemetry](https://github.com/mishmash-io/opentelemetry-server-embedded)). Sometimes we customize the original project's code to better suit our needs and we're publishing our patches here.

Additionally, we're sharing some plugins and extensions that we've developed for or around the original projects.

In more technical terms - this repository contains a build process that:
1. Fetches the original code of a number of open source projects
2. Applies our patches
3. Rebuilds
4. Retests
5. Packages ***smaller, per-feature*** components that you can ***stack*** together on an ***as-needed basis.***
6. Builds and packages the ***extras*** developed by mishmash.io

You can also find ready-made **stacks** for common use case scenarios.

> [!IMPORTANT]
> This repository is a **Work in progress!**
>
> A number of **stacks** we've accumulated internally are not published or not documented fully yet.
>
> Use the `watch` button above to get updates on progress.

#### In this README you will find:
- **The motivation:** [why do we patch and rebuild?](#why-do-we-rebuild-other-open-source-projects)
- **The goals and principles:** [what are we changing?](#summary-of-whats-modified)
- **The rules we follow:**
  - [When patching](#patching-process-outline)
  - [When publishing](#publishing-patches)
- **The original projects:**
  - [As a list, with details on what's modified](#the-patched-projects)
- **The stacks:**
  - [Listed, with links for details](#the-stacks)
  - [Various ways of using a stack](#using-the-stacks)
  - [How to stay current with updates](#getting-updates)
- ***The mishmash.io extras:***
  - [Get more out of the stacks](#plugins-extensions-and-extras)
- **The repository:**
  - [How to build your own stack](#modifying-the-stacks)
- **The background:**
  - [About mishmash.io](#about-mishmashio)

## Why do we rebuild other open source projects?

> [!NOTE]
> Three major reasons why we customize and rebuild other open source projects:
> - Publishing secure software
>   
>   We update code and dependencies to latest versions, especially when new
>   vulnerabilities are reported and **fixes are published.**
>   
>   As our products have a number of open source dependencies it is crucial for us to have a **secure software supply chain.**
> - Unfied set of dependencies
>   
>   We modify open source projects to use the same set of dependencies (and their versions).
>   
>   **Shorter BOMs** are better.
> - Minimal software packages
>   
>   We break down larger open source projects with multiple features into
>   smaller, 'per-feature' modules that can be used **on an as-needed basis.**
>   
>   Independent services **scale better.** 

For example, our distributed database **mishmash io** uses some core functionalities from [Apache HDFS](https://hadoop.apache.org/docs/stable/hadoop-project-dist/hadoop-hdfs/HdfsDesign.html), namely, to manage cold- or hot-storage disks; to replicate large data blocks across zones and clusters; and to provide **zero-copy** data access. Apache HDFS is part of [Apache Hadoop](https://hadoop.apache.org/) and comes with many more features (such as REST-based management APIs or Web GUI apps) and these extra features come with their additional code and dependencies.

By splitting HDFS into smaller, feature-based modules we **minimize image sizes** of software we publish, limiting dependencies to only those that are actually used. Simultaneously, we also **reduce the attack surface.**

Furthermore, as we officially support all software that we deliver, including its dependencies - we would like to make our lives easier by supporting as little code as possible. Therefore, we modify open source projects to use a single dependency for logging; another single dependency for networking; and so on.

## Summary of what's modified

Details about changes we've done to each individual open source package are documented separately (see a list below), but in general, our modifications fall into a few categories:

- Upgrading a dependency to its currently maintained version
  
  When the new dependency has an ***incompatible API*** we do modify the code that uses it. For example, upgrading `jetty` from version 9 to 12 (latest version of `jetty` at the time of writing this README) requires changes to `import` statements, method calls into `jetty` APIs, web servlets configuration and more.
  
  Alternatively, if the new dependency version does not include ***breaking changes*** - we do not modify the original project's code. We only rebuild and retest to be sure it works.

- Splitting code into per-functionality packages
  
  Breaking code apart usually requires ***refactoring*** - moving classes to new `packages` and potentially also changing a method's accessibility (making it `public` for example). For such changes to work we also refactor related test code.
  
  For example, we split [Apache ZooKeeper](https://zookeeper.apache.org/) into `minimal client`, `minimal server`, `cli` and a few other packages. This requires some classes to be moved to different `java packages` to make sure that the `minimal client` bundle does not use code from `minimal server.`

For more details keep reading this document and follow the links to each artifact's docs.

## Patching process outline

Typically, we begin by experimenting (internally) with the original source code to get an idea of how much changes will be needed. This is then weighed against the gains, with **security,** for example - influencing strongly in its favor.

Should we decide to proceed with patching the original code - we evaluate our changes and re-test them within the broader ecosystem of open source that we use. Or in other words - we re-test all open source distributed computing **stacks** that we use.

The steps above are still done internally (nothing gets published) until tests show that everything works as expected.

> [!TIP]
> During a rerun of tests we also collect telemetry data and analyze it to see if our changes hurt performance.
>
> To find out more on how we do this, and how you might apply a similar practice to your own software development - check out [Analytics tools for OpenTelemetry GitHub repository.](https://github.com/mishmash-io/opentelemetry-server-embedded).

As an extra precaution at this point we also modify the original source code to report a `patched version` to avoid confusion in production deployments.

Open source projects often include APIs that can be interrogated when the user (or an admin) needs to **verify what software version is running** on a particular server. To make sure instances of popular projects modified by **mishmash.io** are not mistaken for their original versions we also patch these APIs to respond accordingly (more on this in the project-specific docs).

## Publishing patches

Once we're confident our code changes are functional and safe to use - we publish the code here. The **stacks** are rebuilt and retested once more, with all relevant information - like build and test logs, dependency provenance, etc - saved and made available.

Publishing binaries on other repositories (such as `maven central` or `DockerHub`) only happens when:

- A new version of the original open source project is released
- A dependency of the original open source project is upgraded because of a newly discovered vulnerability in it

Or in other words - we'll only release binaries when the original open source project releases a new version or when the security of its current version is compromised.

Find out more about how we do versioning of our patched releases below.

## The Patched Projects

These are the original open source projects that are ***patched*** here:
- Apache ZooKeeper
  
  > Apache ZooKeeper is an effort to develop and maintain an open-source server which enables highly reliable distributed coordination.
  > 
  > ZooKeeper is a centralized service for maintaining configuration information, naming, providing distributed synchronization, and providing group services. [Apache ZooKeeper website](https://zookeeper.apache.org/)
- Apache Hadoop
  
  > The Apache® Hadoop® project develops open-source software for reliable, scalable, distributed computing.
  > 
  > The Apache Hadoop software library is a framework that allows for the distributed processing of large data sets across clusters of computers using simple programming models. [Apache Hadoop website](https://hadoop.apache.org/)

For details on ***changes to the original code*** of the projects above [see these docs.](patched-projects)

The following are original open source projects whose code is not modified, but ***their dependencies might be.*** These projects are rebuilt, retested and potentially ***repackaged:***

(Coming soon)

## The Stacks

***Stacks*** built here typically provide one specific functionality that you might need when developing your distributed and clustered system. Here's a quick overview, with functionalities by category:

- ***Cloud integration*** blocks
  
  Together, these blocks form a base layer of commonly used functionality and allow you to integrate your clustered software with ***major public clouds*** (like Azure, AWS and GCP) or ***upgrade to next-generation technologies:***
  - ***Configure*** (and reconfigure) your system with modern automation tools
  - Discover the ***topology*** of a running cluster across regions and availability zones and then optimize ***data and task placement.***
  - Add ***backward-compatible, fail-over RPC*** calls with established libraries (such as Protocol Buffers) or the newer, memory-efficient [Apache Arrow.](https://arrow.apache.org/)
  - ***Integrate the security*** of your cluster ***with the IAM of choice*** - OpenID Connect/UMA (like [Keycloak](https://www.keycloak.org/)) or the IAMs of major public clouds.

- ***Quorum*** services
  
  Use the ***quorum stacks*** when you need to coordinate a number of cluster nodes:
  - Ensure nodes have a ***consistent view of a shared state***
  - Synchronize processes running on separate nodes with ***distributed locks, barriers, and more***
  - Develop algorithms where ***nodes have to agree***
  - Distribute and manage ***partitioned resources,*** execute ***massively-parallel tasks*** on them

- ***Data*** management
  
  Conquering the load and performance of data-intensive algorithms needs dividing the input. Store, distribute and process data with these stacks:
  - ***Load-balance disks*** for scalable IOPS, ***tier*** your disks to optimize cost
  - Break large and growing data sets into ***blocks of optimal size*** so that you can evenly split the work across multiple ***compute slots***
  - Process blocks with ***data locality ('zero-copy')***
  - ***Replicate*** blocks throughout your cluster nodes for higher data locality ratio
  - Dynamically ***rebalance*** the placement of data block replicas
  - Combine with ***columnar file formats*** (such as [Apache ORC](https://orc.apache.org/) and [Apache Arrow](https://arrow.apache.org/))  for even better performance. ***Note:*** some of these file formats feature additional performance gains - like ***bloom filters,*** for example
  
- Distributed ***transactions***
  
  (Coming soon)

## Using the stacks

For a quick test or playground experiments you can launch individual services or small clusters using our pre-built images and deployment scripts. (Coming soon)

In production environments we recoomend either:
- Using the stacks ***programmatically*** (embed inside your app)
  
  Choose the functionality you need and add its dependencies to your project. Then, inside your code, initialize and use the functionality through the provided interfaces. (Docs coming soon)

- Combine functionalities into ***custom images*** using our ***builders***
  
  Pack stacks that you need to run side-by-side, but independently of your app, into your own images. Then launch tailored clusters. (Docs coming soon)

Depending on your architecture both approaches have their pros and cons.

## Getting updates

(Coming soon)

## Plugins, extensions and extras

> [!NOTE]
> These packages are not part of the original open source projects - they're developed by **mishmash.io** to complement and extend the functionality.
> 
> Their source code is published in this repository.

- Login modules and SASL mechanisms for popular IAMs.
  
  SASL (Simple Authentication and Security Layer) is a framework that allows 
  networking protocols (such as RPC implementations) to offer configurable authentication and security. That is - you provide a security ***mechanism*** and protocols operate with it.
  
  It is often used with Kerberos as a mechanism, but with this package you can plug in OpenID Connect, UMA or the IAMs of major public clouds as ***SASL mechanisms.*** [Go to the IAM SASL provider.](misc/openid/)

## Modifying the stacks

(Coming soon)

# About mishmash.io

[![GitHub followers](https://img.shields.io/github/followers/mishmash-io)](https://github.com/mishmash-io) [![Bluesky posts](https://img.shields.io/bluesky/posts/mishmash.io)](https://bsky.app/profile/mishmash.io) [![GitHub Discussions](https://img.shields.io/github/discussions/mishmash-io/about?logo=github&logoColor=white)](https://github.com/orgs/mishmash-io/discussions) [![Discord](https://img.shields.io/discord/1208043287001169990?logo=discord&logoColor=white)](https://discord.gg/JqC6VMZTgJ)

(Coming soon)
