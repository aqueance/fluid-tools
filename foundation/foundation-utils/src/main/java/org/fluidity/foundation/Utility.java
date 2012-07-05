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

package org.fluidity.foundation;

/**
 * Abstract superclass that prevents instantiation of its subclasses.
 * <h3>Usage</h3>
 * <pre>
 * public final <span class="hl2">MyUtility</span> extends <span class="hl1">Utility</span> {
 *
 *     private <span class="hl2">MyUtility</span>() { }
 *
 *     &hellip;
 * }
 * </pre>
 */
public abstract class Utility {

    protected Utility() {
        throw new UnsupportedOperationException(String.format("No instance allowed of %s", getClass()));
    }
}
