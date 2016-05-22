### What is Fluid Tools?

Fluid Tools is a compact framework for software craftsmen aimed at making software composition a reality, where relatively independent, simple components are organized into a system of any complexity.

In practical terms, Fluid Tools is a set of Java libraries that help you write _less code_, and it does so not by adding but by _removing_ barriers, thus giving you more freedom to design your code. The tool itself tends _not_ to get in your way, hence the term _fluid_.

We strive for and encourage the highest level of clarity, minimalism, and object-oriented design with a hint of functional programming, where applications are composed of small, stateless, collaborating objects with [clearly defined responsibility](http://en.wikipedia.org/wiki/Single_responsibility_principle), no [dependence on detail](http://en.wikipedia.org/wiki/Dependency_inversion_principle), no feature bloat, and no bad surprises.

### Feature Highlights

#### Refactoring-Friendly Dependency Injection

Unlike other tools out there with similar functionality, our dependency injection containers require no [XML configuration](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-factory-metadata), no [manual](http://tapestry.apache.org/tapestry-ioc-modules.html#TapestryIoCModules-AutobuildingServices) [bindings](https://github.com/google/guice/wiki/Bindings) and no [explicit manifest entries](http://tapestry.apache.org/autoloading-modules.html) to keep in sync with the code, no [run-time scanning](http://docs.spring.io/spring/docs/3.0.0.RC2/spring-framework-reference/html/ch03s10.html?ref=driverlayer.com/web) or [programmatic registration](http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-java-instantiating-container-register) to find components, no decision to make about which [dependency injection style](http://picocontainer.com/injection.html) to adopt, and hardly any thought wasted on designing [hierarchies of containers](http://liferepo.blogspot.hu/2014/03/scoping-dependency-injection.html).

Change your design and architecture freely as your concepts, ideas, and requirements evolve: Fluid Tools will keep up with you.

#### Recursive Archives Without Custom Class Loader

Unlike most solutions to packaging dependencies in a single Java archive, our solution *does not flatten* the dependency tree and requires *no special class loader* to access nested archives at *any level* of nesting, and preserves URL metadata and stream semantics *without* assuming the top level archive to be a file, which mean it can come from the network or from a database.

#### OSGi Applications

OSGi is an amazing technology if you use it right. As with everything designed by a committee, it is bloated and can be unwieldy at times. It has an excellent coarse grained dependency resolution, however, which, when [used properly](https://www.osgi.org/wp-content/uploads/whiteboard1.pdf), brings the fine grained dependency resolution offered by Fluid Tools to the reach of modular applications.

With its dependency injection integrated with the OSGi service resolution, a Fluid Tools based OSGi application can be lean and light-weight, and with the nested archives facilities in Fluid Tools, you can package your modular applications as a self-containing executable JAR file, for instance a remote server that, when launched locally, loads and bootstraps an embedded OSGi container from the archive itself, and then finds and deploys the bundles nested inside the archive, with their own dependencies nested in the bundle archives themselves.

#### Refactoring-Friendly Configuration Properties

Adapting an application or component to a specific environment often requires loading and processing some sort of configuration. There are certain challenges involved in resolving the mismatch between textual configuration and run-time data types, as well as exposing what configuration a particular component requires so that it *can* be configured at all, which when not resolved can adversely affect the sense of freedom to refactor your components.

Fluid Tools thus offers a refactoring friendly solution to these challenges in terms of a configuration facility that allows you to define the mapping between the configuration property names and your particular property store only, and then to declare component configuration on a per component basis, and for each configurable component to have its configuration injected as a dependency.

This allows the configuration to stay with the component and thus it can be refactored and moved around much more freely than if you would have to also take care of the configuration facilities every time you change the component.

#### Notes on [CDI](http://www.cdi-spec.org/)

Dependency injection as conceived and implemented in Fluid Tools is *not compatible* with CDI, and when CDI was first introduced a decision has been made *not* to rectify that situation. Here's a few salient reasons why:

  1. **Scopes**: CDI comes with a set of pre-defined scopes, which make no sense in Fluid Tools: we don't work with users, sessions, or requests, we work with class loaders, and components and their dependencies.
  1. **Qualifiers**: In CDI, qualifiers are used as a means to declaratively select, directly at the dependency reference, which implementation of a component to resolve that particular reference, whereas in Fluid Tools, qualifiers *along a dependency chain* are used to build a rich context for components to *adapt* to, and selecting a particular implementation is one specific means of adaptation out of possibly many. This concept of contexts is more general in Fluid Tools and quite incompatible with that in CDI.
  1. **Dependencies**: CDI assumes frequent use of [dependencies with the wrong direction](http://docs.jboss.org/cdi/learn/userguide/CDI-user-guide.html#_client_proxies), e.g.. from the application scope to a request scope, and you must explicitly state it when you are doing this right. Fluid Tools, on the other hand, is based on the idea that [dependencies should go from the less stable to the more stable component](https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction#the-basic-problem) and not the other way around.
  1. **Statelessness**: Fluid Tools promotes an application design where the code is held together by a static backbone of stateless components through which data can flow. The instantiation of a static component graph is the rule and stateful components are the exception. CDI, on the other hand, assumes that instantiation of transient stateful components is the rule and statelessness is the exception.

### Getting the Code
The sources for these libraries can be downloaded using [Git](https://git-scm.com/downloads) like so:

```
$ git clone https://github.cob/aqueance/fluid-tools.git fluid-tools
```

**NOTE**: This is work in progress hence no release versions or downloads are available yet and some APIs may still change.

### Building Fluid Tools
Use [Maven](http://maven.apache.org) to build Fluid Tools:

```
$ cd fluid-tools
$ mvn install
```

**NOTE**: You will need Maven 3.1+ to build, or use, these libraries.

#### Generating the Javadocs
The uncompressed Java documentation of Fluid Tools is generated by the following command:

```
$ mvn javadoc:aggregate
```

Once that command completes, the documentation starting page will be `target/site/apidocs/index.html`.

### Documentation
A short [Getting Started guide](https://github.com/aqueance/fluid-tools/wiki/Getting-Started) is provided to get you started with Fluid Tools, while the [User Guide](https://github.com/aqueance/fluid-tools/wiki/User-Guide---Introduction) covers the full spectrum of what you can do with Fluid Tools.

### History
The brief history and context for this project are described on our wiki [home page](https://github.com/aqueance/fluid-tools/wiki/Home).
