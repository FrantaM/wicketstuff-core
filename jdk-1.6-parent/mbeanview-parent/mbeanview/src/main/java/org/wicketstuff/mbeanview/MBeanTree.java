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

import java.io.Serializable;

import javax.management.MBeanServerDelegate;
import javax.management.ObjectName;

import org.apache.wicket.Component;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.DefaultNestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;
import org.wicketstuff.mbeanview.nodes.MBeanNode;
import org.wicketstuff.mbeanview.nodes.MBeanTreeNode;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-02T16:01:58+0200
 */
public final class MBeanTree extends DefaultNestedTree<MBeanTreeNode>
{
	private static final long serialVersionUID = 20130402;

	public MBeanTree(final String id, final ITreeProvider<MBeanTreeNode> provider)
	{
		super(id, provider);
	}

	@Override
	public void renderHead(final IHeaderResponse response)
	{
		super.renderHead(response);
		response.render(CssHeaderItem.forReference(MBeansPanel.STYLE));
	}

	@Override
	public void collapse(final MBeanTreeNode t)
	{
		super.collapse(t);
		this.displayMBean(t);
	}

	@Override
	public void expand(final MBeanTreeNode t)
	{
		super.expand(t);
		this.displayMBean(t);
	}

	private void displayMBean(final MBeanTreeNode t)
	{
		if (t instanceof MBeanNode)
		{
			final ObjectName objectName = ((MBeanNode) t).getObjectName();
			if (objectName != null)
			{
				this.send(this, Broadcast.BUBBLE, new MBeanSelectedEvent(objectName));
			}
		}
	}

	@Override
	protected Component newContentComponent(final String id, final IModel<MBeanTreeNode> node)
	{
		return new TreeFolder(id, this, node);
	}

	public static final class MBeanSelectedEvent implements Serializable
	{
		private static final long serialVersionUID = 20130403;
		private final ObjectName objectName;

		private MBeanSelectedEvent(final ObjectName objectName)
		{
			this.objectName = Args.notNull(objectName, "objectName");
		}

		public ObjectName getObjectName()
		{
			return this.objectName;
		}

	}

	private static final class TreeFolder extends Folder<MBeanTreeNode>
	{
		private static final long serialVersionUID = 20130402;

		public TreeFolder(final String id, final AbstractTree<MBeanTreeNode> tree, final IModel<MBeanTreeNode> model)
		{
			super(id, tree, model);
		}

		@Override
		protected boolean isClickable()
		{
			/* MBeanNode is either mbean (objectName != null) or group (!children.isEmpty()).
			 * In both cases should be clickable. */
			return super.isClickable() || this.getModelObject() instanceof MBeanNode;
		}

		@Override
		protected String getStyleClass()
		{
			final StringBuilder sb = new StringBuilder(super.getStyleClass());

			final MBeanTreeNode model = this.getModelObject();
			if (model instanceof MBeanNode)
			{
				final MBeanNode node = (MBeanNode) model;
				if (node.getObjectName() != null)
				{
					sb.append(" ");
					sb.append("tree-mbean");

					if (MBeanServerDelegate.DELEGATE_NAME.equals(node.getObjectName()))
					{
						sb.append("-delegate");
					}
				}
			}

			return sb.toString();
		}

	}

}
