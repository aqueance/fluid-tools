### What is Fluid Tools?

Fluid Tools is a compact framework for software craftsmen aimed at making software composition a reality, where relatively independent, simple components are organized into a system of any complexity.

In practical terms, Fluid Tools is a set of Java libraries that help you write _less code_, and it does so not by adding but by _removing_ barriers, thus giving you more freedom to design your code. The tool itself tends _not_ to get in your way, hence the term _fluid_.

We strive for and encourage the highest level of clarity, minimalism, and object-oriented design with a hint of functional programming, where applications are composed of small, stateless, collaborating objects with [clearly defined responsibility](http://en.wikipedia.org/wiki/Single_responsibility_principle), no [dependence on detail](http://en.wikipedia.org/wiki/Dependency_inversion_principle), no feature bloat, and no bad surprises.

### Feature Highlights

#### Refactoring-Friendly Dependency Injection

Unlike other tools with similar functionality, our dependency injection containers require no [XML configuration](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-factory-metadata), no [manual](https://tapestry.apache.org/tapestry-ioc-modules.html#TapestryIoCModules-AutobuildingServices) [bindings](https://github.com/google/guice/wiki/Bindings) and no [explicit manifest entries](http://tapestry.apache.org/autoloading-modules.html) to keep in sync with the code, no [run-time scanning](https://docs.spring.io/spring/docs/3.0.0.RC2/spring-framework-reference/html/ch03s10.html) or [programmatic registration](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-java-instantiating-container-register) to find components, no decision to make about which [dependency injection style](http://picocontainer.com/injection.html) to adopt, and hardly any thought wasted on designing [hierarchies of containers](https://liferepo.blogspot.hu/2014/03/scoping-dependency-injection.html).

Change your design and architecture freely as your concepts, ideas, and requirements evolve: Fluid Tools will keep up with you.

#### Refactoring-Friendly Configuration Properties

Adapting an application or component to a specific environment often requires loading and processing some sort of configuration settings, which, when not taken care of properly, may bind the components to their environment, severely reducing the reusability of those components.

Providing a means to painlessly handle component configuration is, therefore, an important aspect of working with components.

In Fluid Tools, component configuration is a matter of defining, *by the component*, a Java interface with annotated getter methods. Mapping those configuration property names to specific values is, on the other hand, a matter of implementing, *by the host application*, another Java interface.

The component defined interfaces and the host supplied property mapping are connected at *run-time* by Fluid Tools, which means the design of your components is *not* impacted by *how* they will get configured, allowing you to freely change your design as you model your domain: Fluid Tools will still keep up with you.

#### Nested Archives

Unlike most solutions to packaging dependencies in a single Java archive, our solution *does not flatten* the dependency tree, requires *no special class loader* to access nested archives at *any level* of nesting, and preserves URL metadata and stream semantics *without* assuming the top level archive to be a file, which mean it can come from e.g., the network or from a database, as well as from the file system.

This approach opens up new possibilities in application deployment, of which self-containing executable archives is just one example.

#### OSGi Applications

OSGi is a great technology – if you use it right. As with everything designed by a committee, it is bloated and can be unwieldy at times, but it has an excellent coarse grained dependency resolution mechanism that, when [used properly](https://www.osgi.org/wp-content/uploads/whiteboard1.pdf), makes the fine grained dependency resolution offered by Fluid Tools available to dynamic modular applications.

With its dependency injection integrated with OSGi service discovery and resolution, Fluid Tools will also generate OSGi bundle metadata based on component dependencies, which makes it possible to change your OSGi bundle structure as freely as you change your component design.

With the [nested archives](#nested-archives) facilities in Fluid Tools, you can then easily package your modular application as a self-containing executable JAR file that, when launched, loads and bootstraps an embedded OSGi container from the archive itself, and then finds and deploys the bundles nested inside that same archive, with the bundles' internal dependencies packaged in their respective archives.

#### Notes on [CDI](http://www.cdi-spec.org/)

Dependency injection as conceived and implemented in Fluid Tools is *not compatible* with CDI, and when CDI was first introduced a decision has been made *not* to rectify that situation. Here's a few salient reasons why:

  1. **Scopes**: Scopes in CDI are heavy-weight constructs, with [lots of code and configuration](http://docs.jboss.org/cdi/spec/1.2/cdi-spec.html#spi). In Fluid Tools, defining a new scope is a matter of setting an annotation parameter.
  1. **Qualifiers**: In CDI, qualifiers are used as a means to declaratively select, *at the dependency reference*, the particular component *implementation* to resolve that particular reference with. In Fluid Tools, qualifiers *along a dependency chain* are used to build a rich context for component *instances* to adapt to, and selecting a particular component implementation to instantiate for that particular context is just *one specific means* of adaptation.
  1. **Dependencies**: Dependency injection in CDI is designed around the assumption that it is quite okay for [dependencies to be unstable](http://docs.jboss.org/cdi/learn/userguide/CDI-user-guide.html#_client_proxies), e.g.. a component in application scope *will* depend on a component in, say, request scope, and indirection is required *by default* to support that pathological case. We believe [dependencies should go from unstable to stable](https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction#the-basic-problem) and not the other way around.
  1. **Statelessness**: CDI assumes that the creation of transient and stateful components is the rule, and stability and statelessness are the exception. We, on the other hand, promote a design where the application is held together by a relatively static graph of stateless components, and application logic is expressed in terms of data flowing through that component graph. Thus in Fluid Tools, the instantiation of stateless, stable components is the rule, and transient or stateful components are the exception.
  1. **Terminology**: Beans are roasted, ground, and then soaked in hot water to extract caffeine from. Components are… not?

### Getting the Code

The sources for these libraries can be downloaded using [Git](https://git-scm.com/downloads) like so:

```console
$ git clone https://github.cob/aqueance/fluid-tools.git fluid-tools
```

**NOTE**: This is work in progress hence no release versions or downloads are available yet and some APIs may still change.

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

A short [Getting Started guide](https://github.com/aqueance/fluid-tools/wiki/Getting-Started) is provided to get you started with Fluid Tools, while the [User Guide](https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction) covers the full spectrum of what you can do with Fluid Tools.

### History

The brief history and context for this project are described on our wiki [home page](https://github.com/aqueance/fluid-tools/wiki/Home).
