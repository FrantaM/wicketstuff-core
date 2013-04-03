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
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.mbeanview.panels.AttributesPanel;
import org.wicketstuff.mbeanview.panels.OperationsPanel;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-03T10:24:02+0200
 */
public class MBeanPanel extends GenericPanel<ObjectName>
{
	private static final long serialVersionUID = 20130403;
	private static final Logger log = LoggerFactory.getLogger(MBeanPanel.class);
	private final IMBeanServerConnectionProvider connection;

	public MBeanPanel(final String id, final IMBeanServerConnectionProvider connection, final ObjectName objectName)
	{
		super(id, Model.of(objectName));

		this.connection = connection;
		this.setOutputMarkupId(true);
		this.add(this.newTabbedPanel("tabs"));
	}

	private TabbedPanel<ITab> newTabbedPanel(final String id)
	{
		final List<ITab> tabs = new ArrayList<ITab>();
		tabs.add(new MBeanInfoTab(Model.of("Attributes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isEnabled(final MBeanInfo mbeanInfo)
			{
				return mbeanInfo.getAttributes().length > 0;
			}

			@Override
			public WebMarkupContainer getPanel(final String panelId, final MBeanInfo mbeanInfo)
			{
				return new AttributesPanel(panelId, connection, getModelObject());
			}

		});
		tabs.add(new MBeanInfoTab(Model.of("Operations"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isEnabled(final MBeanInfo mbeanInfo)
			{
				return mbeanInfo.getOperations().length > 0;
			}

			@Override
			public WebMarkupContainer getPanel(final String panelId, final MBeanInfo mbeanInfo)
			{
				return new OperationsPanel(panelId, mbeanInfo.getOperations());
			}

		});

		return new AjaxTabbedPanel<ITab>(id, tabs)
		{
			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer newLink(final String linkId, final int index)
			{
				return (WebMarkupContainer) super.newLink(linkId, index)
						.setEnabled(((MBeanInfoTab) this.getTabs().get(index)).isEnabled());
			}

		};
	}

	private abstract class MBeanInfoTab extends AbstractTab
	{
		private transient MBeanInfo mbeanInfo;
		private transient WebMarkupContainer panel;

		public MBeanInfoTab(final IModel<String> title)
		{
			super(title);
		}

		private MBeanInfo loadMBeanInfo()
		{
			try
			{
				if (this.mbeanInfo == null)
				{
					this.mbeanInfo = connection.get().getMBeanInfo(getModelObject());
				}
				return this.mbeanInfo;
			}
			catch (final IOException ex)
			{
				log.warn("Cannot read from mbean server.", ex);
				return null;
			}
			catch (final JMException ex)
			{
				log.warn("Cannot retrieve mbean info.", ex);
				return null;
			}
		}

		@Override
		public final WebMarkupContainer getPanel(final String panelId)
		{
			if (this.panel == null)
			{
				final MBeanInfo info = this.loadMBeanInfo();
				if (info != null)
				{
					this.panel = this.getPanel(panelId, info);
				}
				else
				{
					this.panel = new EmptyPanel(panelId);
				}
			}

			return this.panel;
		}

		public final boolean isEnabled()
		{
			return this.isEnabled(this.loadMBeanInfo());
		}

		protected abstract boolean isEnabled(final MBeanInfo mbeanInfo);

		protected abstract WebMarkupContainer getPanel(final String panelId, final MBeanInfo mbeanInfo);

	}

}
