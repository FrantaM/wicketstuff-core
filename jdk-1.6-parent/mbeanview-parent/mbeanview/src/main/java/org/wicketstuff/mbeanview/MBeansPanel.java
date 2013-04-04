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

import javax.management.MBeanServerConnection;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
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

}
