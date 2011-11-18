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

package org.fluidity.composition.types;

import org.fluidity.composition.Component;
import org.fluidity.composition.ComponentGroup;

@Component(api = ComponentApi3.class, automatic = false)
@ComponentGroup
public class GroupMemberComponent3 implements ComponentApi3, ComponentApi4, GroupApi2, GroupApi3 {
    // API: GroupMemberComponent3 [GroupApi1, GroupApi2], ComponentApi3 []
}
