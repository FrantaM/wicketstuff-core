/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ReflectionException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.tree.Tree;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.request.resource.CssResourceReference;

/**
 * Jmx panel, to view and operate the applications mbeans
 *
 * @author Pedro Henrique Oliveira dos Santos
 * @author Franta Mejta
 */
public class MBeansPanel extends Panel
{
	private static final long serialVersionUID = 1L;
	private static final String VIEW_PANEL_ID = "view";
	static final CssResourceReference STYLE = new CssResourceReference(MBeansPanel.class, "mbeanview.css");
	private final IMBeanServerConnectionProvider connection;

	public MBeansPanel(final String id)
	{
		this(id, PlatformProvider.INSTANCE);
	}

	public MBeansPanel(final String id, final IMBeanServerConnectionProvider connection)
	{
		super(id);

		this.add(new MBeanTree("tree", new MBeanNodesProvider(this.connection = connection)));
		this.add(new EmptyPanel(VIEW_PANEL_ID).setOutputMarkupId(true));
	}

	@Override
	public void onEvent(final IEvent<?> event)
	{
		final Object payload = event.getPayload();
		if (payload instanceof MBeanTree.MBeanSelectedEvent)
		{
			final MBeanTree.MBeanSelectedEvent eventPayload = (MBeanTree.MBeanSelectedEvent) payload;
			final MBeanPanel panel = new MBeanPanel(VIEW_PANEL_ID, this.connection, eventPayload.getObjectName());

			this.replace(panel);

			final AjaxRequestTarget ajax = this.getRequestCycle().find(AjaxRequestTarget.class);
			if (ajax != null)
			{
				ajax.add(panel);
			}
		}
	}

	private static final class PlatformProvider implements IMBeanServerConnectionProvider
	{
		private static final long serialVersionUID = 20130403;
		private static final PlatformProvider INSTANCE = new PlatformProvider();

		@Override
		public MBeanServerConnection get()
		{
			return ManagementFactory.getPlatformMBeanServer();
		}

	}

	private class MBeanTree2 extends Tree
	{
		private static final long serialVersionUID = 1L;

		public MBeanTree2(String id, TreeModel model)
		{
			super(id, model);
			getTreeState().expandNode(getModelObject().getRoot());
		}

		@Override
		protected void onNodeLinkClicked(AjaxRequestTarget target, TreeNode node)
		{
			if (node instanceof MbeanNode)
			{
				Component newView = ((MbeanNode) node).getView(VIEW_PANEL_ID);
				newView.setOutputMarkupId(true);
				MBeansPanel.this.replace(newView);
				target.add(newView);
			}
		}

		@Override
		protected Component newNodeIcon(MarkupContainer parent, String id, TreeNode node)
		{
			if (node instanceof DefaultMutableTreeNode)
			{
				DefaultMutableTreeNode mutableNode = (DefaultMutableTreeNode) node;
				if (mutableNode.getChildCount() > 0
						&& (mutableNode.getChildAt(0) instanceof AttributeNode
						|| mutableNode.getChildAt(0) instanceof OperationNode || mutableNode.getChildAt(0) instanceof NotificationNode))
				{
					return new EmptyPanel(id).add(AttributeModifier.replace("style", "width:0;"));
				}
			}
			return super.newNodeIcon(parent, id, node);
		}

	}

	private class MbeanNode extends DefaultMutableTreeNode
	{
		private static final long serialVersionUID = 1L;
		protected ObjectInstance objectInstance;
		protected MbeanServerLocator mBeanServerLocator;
		protected String name;
		protected String keyValue;

		public MbeanNode(String domainName)
		{
			super(domainName);
		}

		public MbeanNode(ObjectInstance objectInstance, String keyValue)
		{
			this.objectInstance = objectInstance;
			this.keyValue = keyValue;
			name = keyValue.split("=")[1];
		}

		public MbeanNode(MbeanNode parent)
		{
			objectInstance = parent.objectInstance;
			mBeanServerLocator = parent.mBeanServerLocator;
			name = parent.name;
			keyValue = parent.keyValue;
		}

		public void setObjectInstance(ObjectInstance objectInstance,
				MbeanServerLocator reachMbeanServer) throws InstanceNotFoundException,
				IntrospectionException, ReflectionException
		{
			this.objectInstance = objectInstance;
			mBeanServerLocator = reachMbeanServer;
			MBeanInfo info = reachMbeanServer.get().getMBeanInfo(objectInstance.getObjectName());
			MBeanAttributeInfo[] beanAttributeInfos = info.getAttributes();
			MBeanOperationInfo[] beanOperationInfos = info.getOperations();
			MBeanNotificationInfo[] beanNotificationInfos = info.getNotifications();
			if (beanAttributeInfos.length > 0)
			{
				add(new AttributesNode(this, beanAttributeInfos));
			}
			if (beanOperationInfos.length > 0)
			{
				add(new OperationsNode(this, beanOperationInfos));
			}
			if (beanNotificationInfos.length > 0)
			{
				DefaultMutableTreeNode notificationsNode = new DefaultMutableTreeNode(
						"Notification");
				add(notificationsNode);
				for (MBeanNotificationInfo beanNotificationInfo : beanNotificationInfos)
				{
					notificationsNode.add(new NotificationNode(this, beanNotificationInfo));
				}
			}
		}

		@Override
		public String toString()
		{
			return name != null && !"".equals(name.trim()) ? name : super.toString();
		}

		public String getKeyValue()
		{
			return keyValue;
		}

		public Component getView(String wicketId)
		{
			return new MBeanTree2(wicketId, new DefaultTreeModel(this));
		}

	}

	private class AttributesNode extends MbeanNode
	{
		private static final long serialVersionUID = 1L;
		private final MBeanAttributeInfo[] beanAttributeInfos;

		public AttributesNode(MbeanNode parent, MBeanAttributeInfo[] beanAttributeInfos)
		{
			super(parent);
			this.beanAttributeInfos = beanAttributeInfos;
			for (MBeanAttributeInfo beanAttributeInfo : beanAttributeInfos)
			{
				add(new AttributeNode(this, beanAttributeInfo));
			}
		}

		@Override
		public Component getView(String id)
		{
			return new AttributeValuesPanel(id, objectInstance.getObjectName(), beanAttributeInfos,
					mBeanServerLocator);
		}

		@Override
		public String toString()
		{
			return "Attributes";
		}

	}

	private class AttributeNode extends MbeanNode
	{
		private static final long serialVersionUID = 1L;
		private final MBeanAttributeInfo attributeInfo;

		public AttributeNode(MbeanNode parent, MBeanAttributeInfo mBeanAttributeInfo)
		{
			super(parent);
			attributeInfo = mBeanAttributeInfo;
		}

		@Override
		public Component getView(String wicketId)
		{
			return new AttributeValuesPanel(wicketId, objectInstance.getObjectName(),
					new MBeanAttributeInfo[]
			{
				attributeInfo
			}, mBeanServerLocator);
		}

		@Override
		public String toString()
		{
			return attributeInfo.getName();
		}

	}

	private class OperationsNode extends MbeanNode
	{
		private static final long serialVersionUID = 1L;
		private final MBeanOperationInfo[] beanOperationInfos;

		public OperationsNode(MbeanNode parent, MBeanOperationInfo[] beanOperationInfos)
		{
			super(parent);
			this.beanOperationInfos = beanOperationInfos;
			for (MBeanOperationInfo beanOperationInfo : beanOperationInfos)
			{
				add(new OperationNode(this, beanOperationInfo));
			}
		}

		@Override
		public Component getView(String id)
		{
			return new OperationsPanel(id, objectInstance.getObjectName(), beanOperationInfos,
					mBeanServerLocator);
		}

		@Override
		public String toString()
		{
			return "Operations";
		}

	}

	private class OperationNode extends MbeanNode
	{
		private static final long serialVersionUID = 1L;
		private final MBeanOperationInfo beanOperationInfo;

		public OperationNode(OperationsNode parent, MBeanOperationInfo mBeanOperationInfo)
		{
			super(parent);
			beanOperationInfo = mBeanOperationInfo;
		}

		@Override
		public Component getView(String wicketId)
		{
			return new OperationsPanel(wicketId, objectInstance.getObjectName(),
					new MBeanOperationInfo[]
			{
				beanOperationInfo
			}, mBeanServerLocator);
		}

		@Override
		public String toString()
		{
			return beanOperationInfo.getName();
		}

	}

	private class NotificationNode extends MbeanNode
	{
		private static final long serialVersionUID = 1L;
		private final MBeanNotificationInfo beanNotificationInfo;

		public NotificationNode(MbeanNode parent, MBeanNotificationInfo mBeanNotificationInfo)
		{
			super(parent);
			beanNotificationInfo = mBeanNotificationInfo;
		}

		@Override
		public String toString()
		{
			return beanNotificationInfo.getName();
		}

	}

}
