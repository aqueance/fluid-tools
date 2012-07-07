/*
 * Copyright (c) 2006-2012 Tibor Adam Varga (tibor.adam.varga on gmail)
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

package org.fluidity.tests;

import java.util.Arrays;
import java.util.List;

import org.fluidity.composition.Component;
import org.fluidity.deployment.osgi.StartLevels;

import org.osgi.framework.Bundle;

@Component
final class StartLevelsImpl implements StartLevels {

    @SuppressWarnings("unchecked")
    public List<List<Bundle>> bundles(final List<Bundle> bundles) {
        return Arrays.asList(bundles.subList(0, 1));
    }

    public int initial(final int maximum) {
        return 0;
    }
}
