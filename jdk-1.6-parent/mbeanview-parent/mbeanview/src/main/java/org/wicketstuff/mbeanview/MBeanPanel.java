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
import java.util.concurrent.Callable;

import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

import org.apache.wicket.event.IEvent;
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
import org.wicketstuff.mbeanview.panels.EventSupport;
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

	@Override
	public void onEvent(final IEvent<?> event)
	{
		final Object eventPayload = event.getPayload();
		if (eventPayload instanceof OperationsPanel.InvokeOperationEvent)
		{
			final OperationsPanel.InvokeOperationEvent payload = (OperationsPanel.InvokeOperationEvent) eventPayload;
			final MBeanOperationInfo operation = payload.getOperation();
			final String[] signature = new String[operation.getSignature().length];
			for (int i = 0, max = signature.length; i < max; ++i)
			{
				signature[i] = operation.getSignature()[i].getType();
			}

			this.onEventCall(payload, new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					return connection.get().invoke(getModelObject(),
							payload.getOperation().getName(),
							payload.getParameters(), signature);
				}

			});
		}
		else if (eventPayload instanceof AttributesPanel.GetAttributeEvent)
		{
			final AttributesPanel.GetAttributeEvent payload = (AttributesPanel.GetAttributeEvent) eventPayload;
			this.onEventCall(payload, new Callable<Object>()
			{
				@Override
				public Object call() throws Exception
				{
					return connection.get().getAttribute(getModelObject(), payload.getAttribute().getName());
				}

			});
		}
	}

	private void onEventCall(final EventSupport payload, final Callable<?> call)
	{
		payload.setException(null);
		payload.setResult(null);

		try
		{
			payload.setResult(call.call());
		}
		catch (final RuntimeMBeanException ex)
		{
			payload.setException(ex.getTargetException());
		}
		catch (final MBeanException ex)
		{
			payload.setException(ex.getTargetException());
		}
		catch (final Exception ex)
		{
			payload.setException(ex);
		}

		if (payload.getException() != null)
		{
			final Throwable payloadException = payload.getException();
			final StackTraceElement[] stack = (new Throwable()).getStackTrace();
			final StackTraceElement[] payloadStack = payloadException.getStackTrace();
			if (stack.length > 0 && payloadStack.length > 0)
			{
				/* First common parent is caller of this method. */
				final StackTraceElement thisCall = stack[1];
				/* Stack contains two additional frames:
				 *  - call to call.call() (above in try)
				 *  - call to MBeanServerConnection in provided call */
				final int removeStack = 2;

				for (int i = removeStack; i < payloadStack.length; ++i)
				{
					if (payloadStack[i].equals(thisCall))
					{
						final int size = i - removeStack;
						final StackTraceElement[] uniqueStack = new StackTraceElement[size];
						System.arraycopy(payloadStack, 0, uniqueStack, 0, size);
						payloadException.setStackTrace(uniqueStack);
						break;
					}
				}
			}
		}
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
				return new AttributesPanel(panelId, mbeanInfo.getAttributes());
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
