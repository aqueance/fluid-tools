/*
 * Copyright (c) 2006-2016 Tibor Adam Varga (tibor.adam.varga on gmail)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fluidity.deployment.cli;

/**
 * Command line application root object. For Fluid Tools based command line applications, this interface takes the place of the public static <code>main</code>
 * method. The command line application simply provides an implementation of this interface that is annotated with {@link
 * org.fluidity.composition.Component @Component}, in a JAR file produced by Maven and processed by the
 * <code>org.fluidity.maven:composition-maven-plugin</code> Maven plugin.
 * <p>
 * The application exits when the call to the {@link #run(String[]) run()} method returns unless the developer has started and failed to stop non-daemon
 * threads.
 * <h3>Usage</h3>
 * <h4>POM</h4>
 * Use the <code>org.fluidity.maven:fluidity-archetype-standalone-jar</code> Maven archetype to generate the command line application wrapper project.
 * <h4>Code</h4>
 * <pre>
 * {@linkplain org.fluidity.composition.Component @Component}
 * final class MyApplication implements <span class="hl1">Application</span> {
 *
 *   public void <span class="hl1">run</span>(final {@linkplain String}[] arguments) throws {@linkplain Exception} {
 *     &hellip;
 *   }
 * }
 * </pre>
 */
public interface Application {

    /**
     * Entry point to the command line application. This method is invoked from the main thread. The application terminates when this method returns unless
     * non-daemon threads were left active.
     *
     * @param arguments the command line arguments.
     *
     * @throws Exception reported to the command launcher.
     */
    void run(String[] arguments) throws Exception;
}
