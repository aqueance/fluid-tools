### What is Fluid Tools?

Fluid Tools is a compact set of libraries aimed at making software composition a reality, where relatively independent,
simple components are organized into a system of any complexity.

In practical terms, Fluid Tools helps you write _less code_, and it does so not by adding but by _removing_ barriers, thus
giving you more freedom to design your code. The tool itself tends _not_ to get in your way, hence the term _fluid_.

We strive for and encourage the highest level of clarity, minimalism, and object-oriented design with a hint of functional
programming, where applications are composed of small, stateless, collaborating objects with [clearly defined responsibility](http://en.wikipedia.org/wiki/Single_responsibility_principle), no [dependence on detail](http://en.wikipedia.org/wiki/Dependency_inversion_principle), no feature bloat, and no bad surprises.

### Feature Highlights

#### Refactoring-Friendly Dependency Injection

Unlike other tools with similar functionality, our dependency injection containers require
no [XML configuration](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-factory-metadata),
no [manual](https://tapestry.apache.org/tapestry-ioc-modules.html#TapestryIoCModules-AutobuildingServices)
[bindings](https://github.com/google/guice/wiki/Bindings) and
no [explicit manifest entries](http://tapestry.apache.org/autoloading-modules.html) to keep in sync with the code,
no [run-time scanning](https://docs.spring.io/spring/docs/3.0.0.RC2/spring-framework-reference/html/ch03s10.html)
or [programmatic registration](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-java-instantiating-container-register)
to find components, no decision to make about which [dependency injection style](http://picocontainer.com/injection.html) to
adopt, and hardly any thought wasted on designing [hierarchies of containers](https://liferepo.blogspot.hu/2014/03/scoping-dependency-injection.html).

Change your design and architecture freely as your concepts, ideas, and requirements evolve: Fluid Tools will keep up with
you.

#### Refactoring-Friendly Configuration Properties

Adapting an application or component to a specific environment often requires loading and processing configuration settings,
which can bind the components to their environment, severely reducing the reusability of those components.

Providing a means to painlessly handle component configuration is, therefore, an important aspect of working with components.

In Fluid Tools, component configuration is a matter of defining, *by the component*, a Java interface with annotated getter
methods. Mapping those configuration property names to specific values is, on the other hand, a matter of implementing, _by
the host application_, another Java interface.

The component defined interfaces and the host supplied property mapping are connected at *run-time* by Fluid Tools, which
means the design of your components is *not* impacted by *how* they will get configured, allowing you to freely change your
design as you model your domain: Fluid Tools will still keep up with you.

#### Nested Archives

Unlike most solutions to packaging dependencies in a single Java archive, our solution *does not flatten* the dependency tree,
requires *no special class loader* to access nested archives at *any level* of nesting, and preserves URL metadata and stream
semantics *without* assuming the top level archive to be a file, which mean it can come from e.g., the network or from a
database, as well as from the file system.

This approach opens up new possibilities in application deployment, of which self-containing executable archives is just one
example.

#### OSGi Applications

OSGi is a great technology – if you use it right. As with everything designed by a committee, it is bloated and can be
unwieldy at times, but it has an excellent coarse grained dependency resolution mechanism that, when
[used properly](https://www.osgi.org/wp-content/uploads/whiteboard1.pdf), extends the fine grained dependency resolution
offered by Fluid Tools to dynamic modular applications.

With its dependency injection integrated with OSGi service discovery and resolution, Fluid Tools will also generate OSGi
bundle metadata based on component dependencies, which makes it possible to change your OSGi bundle structure as freely as
you change your component design.

With the [nested archives](#nested-archives) facilities in Fluid Tools, you can then easily package your modular application
as a self-containing executable JAR file that, when launched, loads and bootstraps an embedded OSGi container from the archive
itself, and then finds and deploys the bundles nested inside that same archive, with the bundles' internal dependencies
packaged in their respective archives.

#### Notes on [CDI](http://www.cdi-spec.org/)

Dependency injection as conceived and implemented in Fluid Tools is *not compatible* with CDI, and when CDI was first
introduced a decision has been made *not* to rectify that situation. Here's a few salient reasons why:

  1. **Scopes**: Scopes in CDI are heavy-weight constructs with
     [lots of code and configuration](http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#scopes). A scope in Fluid Tools
     springs to existence by the simple act of adding a specific annotation to a component.
  1. **Qualifiers**: In CDI, qualifiers are used as a means to declaratively select, *at the dependency reference*, the
     particular component *implementation* to resolve that particular reference with. A quialifier in Fluid Tools qualifies
     the _context_ in which component resolution takes place down the line. A component referring to another does _not_
     need to know what particular implementation it will receive in any particular place in a dependency graph.
  1. **Dependencies**: Dependency injection in CDI is designed around the assumption that it is quite okay for
     [dependencies to be unstable](http://docs.jboss.org/cdi/learn/userguide/CDI-user-guide.html#_client_proxies), e.g.. a
     component in application scope _will_ depend on a component in, say, request scope, and indirection is required _by
     default_ to support that pathological case. We believe
     [dependencies should go from unstable to stable](https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction#the-basic-problem)
     and not the other way around.
  1. **Statelessness**: CDI assumes that the creation of transient and stateful components is the rule, and stability and
     statelessness are the exception. We, on the other hand, promote a design where the application is held together by a
     relatively static graph of stateless components, and application logic is expressed in terms of data flowing through
     that component graph. Thus in Fluid Tools, the instantiation of stateless, stable components is the rule, and transient
     or stateful components are the exception.
  1. **Terminology**: Still on about "beans"… can't we finally move on?

### Using the Libraries

Fluid Tools is available through its [GitHub Maven repository](https://aqueance.github.io/maven/). Please refer to the
[Archetype Catalog](https://github.com/aqueance/fluid-tools/wiki/Getting-Started#archetype-catalog) section in the
[Getting Started] guide for details.

### Getting the Source Code

The sources for these libraries can be downloaded using [Git](https://git-scm.com/downloads) like so:

```console
$ git clone https://github.cob/aqueance/fluid-tools.git fluid-tools
```

**NOTE**: This is work in progress, expect the API to change without notice.

### Building Fluid Tools

Use [Maven](http://maven.apache.org) to build Fluid Tools:

```console
$ cd fluid-tools
$ mvn install
```

**NOTE**: You will need Maven 3.2+ to build, or use, these libraries.

#### Generating the Javadocs

The uncompressed Java documentation of Fluid Tools is generated by the following command:

```console
$ mvn javadoc:aggregate
```

Once that command completes, the documentation starting page will be `target/site/apidocs/index.html`.

### Documentation

A short [Getting Started] guide is provided to get you started with Fluid Tools, while the [User Guide] covers the full
spectrum of what you can do with Fluid Tools.

The most recent API documentation is also available
[online](https://aqueance.github.io/maven/apidocs/org.fluidity.platform-1.0.0-SNAPSHOT/).

### History

The brief history and context for this project are described on our wiki [Home] page.

  [Getting Started]: https://github.com/aqueance/fluid-tools/wiki/Getting-Started
  [User Guide]: https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction
  [Home]: https://github.com/aqueance/fluid-tools/wiki/Home
