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
package org.wicketstuff.mbeanview;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.mbeanview.nodes.DomainNode;
import org.wicketstuff.mbeanview.nodes.MBeanNode;
import org.wicketstuff.mbeanview.nodes.MBeanTreeNode;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-03-29T16:27:04+0100
 */
public class MBeanNodesProvider implements ITreeProvider<MBeanTreeNode>
{
	private static final long serialVersionUID = 20130329;
	private static final Logger log = LoggerFactory.getLogger(MBeanNodesProvider.class);
	private final List<MBeanTreeNode> nodes = new ArrayList<MBeanTreeNode>();

	public MBeanNodesProvider(final IMBeanServerConnectionProvider connection)
	{
		Args.notNull(connection, "connection");

		final MBeanServerConnection conn = connection.get();
		try
		{
			for (final String domain : conn.getDomains())
			{
				final List<MBeanNode> children = new ArrayList<MBeanNode>();
				for (final ObjectName name : conn.queryNames(null, new ObjectName(domain + ":*")))
				{
					appendChild(children, name);
				}

				this.nodes.add(new DomainNode(domain, children));
			}
		}
		catch (final IOException ex)
		{
			log.warn("Cannot query mbeans from mbean server.", ex);
		}
		catch (final MalformedObjectNameException ex)
		{
			throw new AssertionError(ex);
		}
	}

	private static void appendChild(final List<MBeanNode> nodes, final ObjectName objectName)
	{
		final MBeanNode child = makeChildTree(objectName);
		for (final MBeanNode node : nodes)
		{
			final MBeanNode merged = MBeanNode.merge(node, child);
			if (merged != null)
			{
				nodes.set(nodes.indexOf(node), merged);
				return;
			}
		}

		nodes.add(child);
	}

	private static MBeanNode makeChildTree(final ObjectName objectName)
	{
		final MBeanNode child = new MBeanNode(objectName);
		final NavigableMap<String, String> childMap = child.getNodeProperties();
		final List<String> childKeys = new ArrayList<String>(childMap.keySet());

		if (childKeys.size() == 1)
		{
			return child;
		}

		MBeanNode parent = null;
		for (int i = childKeys.size() - 2; i >= 0; --i)
		{
			final String key = childKeys.get(i);
			final NavigableMap<String, String> nodeMap = childMap.headMap(key, true);
			final List<MBeanNode> children = Collections.singletonList(parent != null ? parent : child);
			parent = new MBeanNode(nodeMap, children);
		}

		return parent;
	}

	@Override
	public Iterator<? extends MBeanTreeNode> getRoots()
	{
		return this.nodes.iterator();
	}

	@Override
	public boolean hasChildren(final MBeanTreeNode node)
	{
		return !node.getChildren().isEmpty();
	}

	@Override
	public Iterator<? extends MBeanTreeNode> getChildren(final MBeanTreeNode node)
	{
		return node.getChildren().iterator();
	}

	@Override
	public IModel<MBeanTreeNode> model(MBeanTreeNode object)
	{
		return Model.of(object);
	}

	@Override
	public void detach()
	{
	}

}
