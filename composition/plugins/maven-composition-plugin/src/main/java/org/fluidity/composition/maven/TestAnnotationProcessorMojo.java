/*
 * Copyright (c) 2006-2011 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.composition.maven;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * @goal process-test-annotations
 * @phase process-test-classes
 * @requiresDependencyResolution test
 * @threadSafe
 *
 * @author Tibor Varga
 */
public class TestAnnotationProcessorMojo extends AbstractAnnotationProcessorMojo {

    public void execute() throws MojoExecutionException {
        processDirectory(new File(build().getTestOutputDirectory()));
    }

    @Override
    protected String getProjectNameId() {
        return String.format("%sTest", super.getProjectNameId());
    }
}