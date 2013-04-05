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
package org.wicketstuff.mbeanview.panels;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectName;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-03T10:53:24+0200
 */
public class AttributesPanel extends GenericPanel<ObjectName>
{
	private static final long serialVersionUID = 20130403;
	private static final MetaDataKey<MBeanAttributeInfo> MBEAN_ATTRIBUTE = new MetaDataKey<MBeanAttributeInfo>()
	{
		private static final long serialVersionUID = 1L;
	};
	private final MBeanAttributeInfo[] attributes;

	public AttributesPanel(final String id, final MBeanAttributeInfo[] attributes)
	{
		super(id);
		this.attributes = attributes;
	}

	@Override
	protected void onInitialize()
	{
		super.onInitialize();
		this.add(this.newAttributesList("attribute", attributes));
	}

	private WebMarkupContainer newAttributesList(final String id, final MBeanAttributeInfo[] attributes)
	{
		final RepeatingView view = new RepeatingView(id);
		for (final MBeanAttributeInfo attribute : attributes)
		{
			final WebMarkupContainer row = new WebMarkupContainer(view.newChildId());
			row.add(new Label("name", attribute.getName()));

			final GetAttributeEvent event = new GetAttributeEvent(attribute);
			this.send(this, Broadcast.BUBBLE, event);

			final Object result = event.getException() != null ? event.getException() : event.getResult();
			final ClassInfo ci = ClassInfo.of(attribute.getType());
			final ResultPanel rp = new ResultPanel("value", result, ci.getClassType(), true, attribute.isWritable());
			rp.setMetaData(MBEAN_ATTRIBUTE, attribute);
			row.add(rp);

			view.add(row);
		}

		return view;
	}

	@Override
	public void onEvent(final IEvent<?> event)
	{
		if (event.getPayload() instanceof ResultPanel.SetValueEvent)
		{
			final ResultPanel.SetValueEvent payload = (ResultPanel.SetValueEvent) event.getPayload();
			final ResultPanel panel = (ResultPanel) event.getSource();
			final MBeanAttributeInfo attribute = panel.getMetaData(MBEAN_ATTRIBUTE);

			final SetAttributeEvent setAttributeEvent = new SetAttributeEvent(attribute, payload.getValue());
			this.send(this, Broadcast.BUBBLE, setAttributeEvent);

			payload.setException(setAttributeEvent.getException());
		}
		super.onEvent(event);
	}

	public static final class GetAttributeEvent extends EventSupport
	{
		private static final long serialVersionUID = 20130404;
		private final MBeanAttributeInfo attribute;

		public GetAttributeEvent(final MBeanAttributeInfo attribute)
		{
			this.attribute = attribute;
		}

		public MBeanAttributeInfo getAttribute()
		{
			return attribute;
		}

	}

	public static final class SetAttributeEvent extends EventSupport
	{
		private static final long serialVersionUID = 20130405;
		private final MBeanAttributeInfo attribute;
		private final Object value;

		public SetAttributeEvent(final MBeanAttributeInfo attribute, final Object value)
		{
			this.attribute = attribute;
			this.value = value;
		}

		public MBeanAttributeInfo getAttribute()
		{
			return this.attribute;
		}

		public Object getValue()
		{
			return value;
		}

	}

}
