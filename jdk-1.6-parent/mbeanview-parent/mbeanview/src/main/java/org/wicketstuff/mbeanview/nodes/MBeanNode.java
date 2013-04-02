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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.ObjectName;

/**
 * Tree node representing MBean or group of MBeans (or both).
 *
 * @author Franta Mejta
 * @sa.date 2013-03-29T17:27:41+0100
 */
public class MBeanNode extends MBeanTreeNode
{
	private static final long serialVersionUID = 20130329;
	/**
	 * ObjectName of this MBean.
	 * Nullable.
	 */
	private final ObjectName objectName;
	/**
	 * Properties of current node.
	 * Sorted version of objectName#keyPropertyList
	 */
	private final NavigableMap<String, String> properties;
	/**
	 * Children of current node/mbean.
	 */
	private final List<MBeanNode> children;

	public MBeanNode(final ObjectName objectName)
	{
		this(objectName, Collections.<MBeanNode>emptyList());
	}

	public MBeanNode(final ObjectName objectName, final List<MBeanNode> children)
	{
		this.objectName = objectName;
		this.children = children;
		this.properties = new TreeMap<String, String>(new PropertyComparator());
		this.properties.putAll(objectName.getKeyPropertyList());
	}

	public MBeanNode(final NavigableMap<String, String> nodeProperties, final List<MBeanNode> children)
	{
		this.objectName = null;
		this.children = children;
		this.properties = nodeProperties;
	}

	public ObjectName getObjectName()
	{
		return this.objectName;
	}

	@Override
	public List<MBeanNode> getChildren()
	{
		return this.children;
	}

	public NavigableMap<String, String> getNodeProperties()
	{
		return new TreeMap<String, String>(this.properties);
	}

	@Override
	public String toString()
	{
		return this.properties.get(this.properties.lastKey());
	}

	public static MBeanNode merge(final MBeanNode lhs, final MBeanNode rhs)
	{
		if (!lhs.getNodeProperties().equals(rhs.getNodeProperties()))
		{
			return null;
		}

		final List<MBeanNode> newNodes = new ArrayList<MBeanNode>();
		final List<MBeanNode> lhsNodes = new ArrayList<MBeanNode>(lhs.getChildren());
		final List<MBeanNode> rhsNodes = new ArrayList<MBeanNode>(rhs.getChildren());

		mergeLoop:
		while (!lhsNodes.isEmpty() && !rhsNodes.isEmpty())
		{
			for (final MBeanNode lhsNode : lhsNodes)
			{
				for (final MBeanNode rhsNode : rhsNodes)
				{
					final MBeanNode merged = merge(lhsNode, rhsNode);
					if (merged != null)
					{
						newNodes.add(merged);
						lhsNodes.remove(lhsNode);
						rhsNodes.remove(rhsNode);
						continue mergeLoop;
					}
				}
			}

			break;
		}

		for (final MBeanNode node : lhsNodes)
		{
			newNodes.add(node);
		}
		for (final MBeanNode node : rhsNodes)
		{
			newNodes.add(node);
		}

		// @todo objectName

		return new MBeanNode(lhs.getNodeProperties(), newNodes);
	}

	private static final class PropertyComparator implements Comparator<String>, Serializable
	{
		private static final long serialVersionUID = 20130402;
		private static String[] preferredOrder =
		{
			"type", "j2eeType", "class", "application", "name"
		};
		private final ConcurrentMap<String, Integer> order = new ConcurrentHashMap<String, Integer>();

		public PropertyComparator()
		{
			for (int i = 0; i < preferredOrder.length; ++i)
			{
				this.order.put(preferredOrder[i], i);
			}
		}

		@Override
		public int compare(final String o1, final String o2)
		{
			this.order.putIfAbsent(o1, this.order.size());
			this.order.putIfAbsent(o2, this.order.size());

			final Integer o1order = this.order.get(o1);
			final Integer o2order = this.order.get(o2);
			return o1order.compareTo(o2order);
		}

	}

}
