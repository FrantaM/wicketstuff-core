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

import java.util.Collections;
import java.util.List;
import org.apache.wicket.util.lang.Args;

/**
 * Root node representing mbeans domain.
 *
 * @author Franta Mejta
 * @sa.date 2013-03-29T17:14:15+0100
 */
public final class DomainNode extends MBeanTreeNode
{
	private static final long serialVersionUID = 20130329;
	/**
	 * Domain name.
	 */
	private final String domain;
	/**
	 * Domain mbeans.
	 */
	private final List<MBeanNode> children;

	/**
	 * Constructor.
	 *
	 * @param domain Domain name.
	 * @param children List of domain mbeans.
	 */
	public DomainNode(final String domain, final List<MBeanNode> children)
	{
		this.domain = Args.notEmpty(domain, "domain");
		this.children = Collections.unmodifiableList(Args.notNull(children, "children"));
	}

	@Override
	public List<MBeanNode> getChildren()
	{
		return this.children;
	}

	@Override
	public String toString()
	{
		return this.domain;
	}

}
