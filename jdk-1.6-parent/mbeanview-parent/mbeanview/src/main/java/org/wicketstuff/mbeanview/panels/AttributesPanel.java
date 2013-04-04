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

import org.apache.wicket.event.Broadcast;
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
			row.add(new ResultPanel("value", result, ci.getClassType(), true, attribute.isWritable()));

			view.add(row);
		}

		return view;
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

}
