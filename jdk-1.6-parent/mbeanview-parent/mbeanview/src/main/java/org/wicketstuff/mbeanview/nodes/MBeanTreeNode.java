/*
 * Copyright 2013 Franta Mejta.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wicketstuff.mbeanview.nodes;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-03-29T17:13:38+0100
 */
public abstract class MBeanTreeNode implements Serializable
{
	public List<? extends MBeanTreeNode> getChildren()
	{
		return Collections.emptyList();
	}

}
