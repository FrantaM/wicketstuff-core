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

import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.PanelCachingTab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.Model;
import org.wicketstuff.mbeanview.panels.AttributesPanel;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-03T10:24:02+0200
 */
public class MBeanPanel extends GenericPanel<ObjectName>
{
	private static final long serialVersionUID = 20130403;
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
		tabs.add(new AbstractTab(Model.of("Attributes"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(final String panelId)
			{
				return new AttributesPanel(panelId, connection, getModelObject());
			}

		});
		tabs.add(new AbstractTab(Model.of("Operations"))
		{
			private static final long serialVersionUID = 1L;

			@Override
			public WebMarkupContainer getPanel(final String panelId)
			{
				return new org.wicketstuff.mbeanview.panels.OperationsPanel(panelId, connection, getModelObject());
			}

		});

		final List<ITab> cachedTabs = new ArrayList<ITab>(tabs.size());
		for (final ITab tab : tabs)
		{
			cachedTabs.add(new PanelCachingTab(tab));
		}

		return new AjaxTabbedPanel<ITab>(id, cachedTabs);
	}

}
