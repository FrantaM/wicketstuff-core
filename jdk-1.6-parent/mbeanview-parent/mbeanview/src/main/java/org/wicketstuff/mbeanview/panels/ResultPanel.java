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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-04T10:20:42+0200
 */
final class ResultPanel extends Panel
{
	private static final long serialVersionUID = 20130404;
	private static final String CONTAINER_ID = "result-placeholder";
	private final boolean outline;

	public ResultPanel(final String id, final Object result)
	{
		this(id, result, false, false);
	}

	public ResultPanel(final String id, final Object result, final boolean outline, final boolean editable)
	{
		super(id);

		this.outline = outline;
		this.add(this.resultContainer(result));
	}

	private WebMarkupContainer resultContainer(final Object result)
	{
		if (result == null)
		{
			return this.resultContainerText("null");
		}
		if (result instanceof Throwable)
		{
			final StringWriter sw = new StringWriter();
			((Throwable) result).printStackTrace(new PrintWriter(sw));

			return this.resultContainerText(sw.getBuffer());
		}
		if (result instanceof CompositeData)
		{
			return this.resultContainerText("<composite data>");
		}
		if (result instanceof TabularData)
		{
			return this.resultContainerText("<tabular data>");
		}
		if (result.getClass().isArray())
		{
			if (CompositeData.class.isAssignableFrom(result.getClass().getComponentType()))
			{
				return this.resultContainerText("<composite data array>");
			}

			final Object[] array = new Object[Array.getLength(result)];
			for (int i = 0; i < array.length; ++i)
			{
				array[i] = Array.get(result, i);
			}

			return this.resultContainerText(Arrays.deepToString(array));
		}

		return this.resultContainerText(result);
	}

	private WebMarkupContainer resultContainerText(final Object result)
	{
		final Fragment f = new Fragment(CONTAINER_ID, "result-text", this);

		final String value = String.valueOf(result);
		if (this.outline)
		{
			int lineEnd = value.indexOf('\n');
			if (lineEnd < 0)
			{
				lineEnd = value.indexOf('\r');
			}

			f.add(new Label("result", lineEnd < 0 ? value : value.substring(0, lineEnd)));
		}
		else
		{
			f.add(new MultiLineLabel("result", value));
		}

		return f;
	}

}
