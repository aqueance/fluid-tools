### What is Fluid Tools?

Fluid Tools is a compact framework for software craftsmen aimed at making software composition a reality, where relatively independent, simple components are organized into a system of any complexity.

In practical terms, Fluid Tools is a set of Java libraries that help you write _less code_, and it does so not by adding but by _removing_ barriers, thus giving you more freedom to design your code. The tool itself tends _not_ to get in your way, hence the term _fluid_.

We strive for and encourage the highest level of clarity, minimalism, and object-oriented design with a hint of functional programming, where applications are composed of small, stateless, collaborating objects with [clearly defined responsibility](http://en.wikipedia.org/wiki/Single_responsibility_principle), no [dependence on detail](http://en.wikipedia.org/wiki/Dependency_inversion_principle), no feature bloat, and no bad surprises.

Unlike other tools out there with similar functionality, our dependency injection containers require no [XML configuration](http://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html#beans-factory-metadata), no [manual](http://tapestry.apache.org/tapestry-ioc-modules.html#TapestryIoCModules-AutobuildingServices) [bindings](https://github.com/google/guice/wiki/Bindings) and no [explicit manifest entries](http://tapestry.apache.org/autoloading-modules.html) to keep in sync with the code, no [run-time scanning](http://docs.spring.io/spring/docs/3.0.0.RC2/spring-framework-reference/html/ch03s10.html?ref=driverlayer.com/web) or [programmatic registration](http://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-java-instantiating-container-register) to find components, no decision to make about which [dependency injection style](http://picocontainer.com/injection.html) to adopt, and hardly any thought wasted on designing [hierarchies of containers](http://liferepo.blogspot.hu/2014/03/scoping-dependency-injection.html).

Change your design and architecture freely as your concepts, ideas, and requirements evolve: Fluid Tools will keep up with you.

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
The brief history and context for this project are described our wiki [home page](https://github.com/aqueance/fluid-tools/wiki/Home).
