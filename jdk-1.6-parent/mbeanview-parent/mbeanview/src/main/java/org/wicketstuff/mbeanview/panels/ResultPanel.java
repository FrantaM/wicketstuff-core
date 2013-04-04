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
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-04T10:20:42+0200
 */
final class ResultPanel extends Panel
{
	private static final long serialVersionUID = 20130404;
	private static final String CONTAINER_ID = "result-placeholder";
	private static final int OUTLINE_LENGTH = 120;
	private final ModalWindow detailWindow;
	private final boolean outline;
	private final boolean editable;

	public ResultPanel(final String id, final Object result, final Class<?> resultType)
	{
		this(id, result, resultType, false, false);
	}

	public ResultPanel(final String id, final Object result, Class<?> resultType, final boolean outline, final boolean editable)
	{
		super(id);

		this.outline = outline;
		this.editable = editable;

		this.add((this.detailWindow = new ModalWindow("detail")).setVisibilityAllowed(this.outline));
		this.add(this.resultContainer(result, resultType));
	}

	private WebMarkupContainer resultContainer(final Object result, final Class<?> resultType)
	{
		if (this.outline)
		{
			return this.resultContainerOutline(result, resultType);
		}

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

	private WebMarkupContainer resultContainerOutline(final Object result, final Class<?> resultType)
	{
		final Fragment f = new Fragment(CONTAINER_ID, "result-text", this);

		if (result == null)
		{
			if (this.editable)
			{
				f.add(new OutlineTextField<Serializable>("result", Model.of(), resultType)
						.add(AttributeModifier.replace("placeholder", "null")));
			}
			else
			{
				f.add(new Label("result", "null"));
			}
		}
		else if (result instanceof CompositeData || result instanceof TabularData)
		{
			f.add(new OutlineLink("result", result, result.getClass().getName()));
		}
		else if (result instanceof Throwable)
		{
			f.add(new OutlineLink("result", result, ((Throwable) result).getLocalizedMessage()));
		}
		else if (result.getClass().isArray())
		{
			final String label = String.format(this.getLocale(), "%s items", Array.getLength(result)); // l10n
			f.add(new OutlineLink("result", result, label));
		}
		else
		{
			final String label = String.valueOf(result);
			final int length = Math.min(label.length(), OUTLINE_LENGTH);

			if (length < label.length())
			{
				final String linkLabel = String.format("%s&hellip;", label.substring(0, length));
				f.add(new OutlineLink("result", result, linkLabel));
			}
			else if (this.editable && result instanceof Serializable)
			{
				f.add(new OutlineTextField<Serializable>("result", Model.of((Serializable) result), resultType));
			}
			else
			{
				f.add(new Label("result", label));
			}
		}

		return f;
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
				if (lineEnd < 0)
				{
					lineEnd = value.length();
				}
			}
			lineEnd = Math.min(lineEnd, OUTLINE_LENGTH);

			final boolean displayDetail = lineEnd < value.length();

			f.add(new Label("result", value.substring(0, lineEnd)));
			f.add(new AjaxLink<Void>("display-detail")
			{
				private static final long serialVersionUID = 1L;

				@Override
				public void onClick(final AjaxRequestTarget target)
				{
					detailWindow.setContent(new MultiLineLabel(detailWindow.getContentId(), value));
					detailWindow.setVisibilityAllowed(true);
					detailWindow.show(target);
				}

				@Override
				public boolean isVisible()
				{
					return displayDetail;
				}

			});
		}
		else
		{
			f.add(new MultiLineLabel("result", value));
			f.add(new EmptyPanel("display-detail"));
		}

		return f;
	}

	private final class OutlineTextField<T> extends TextField<T>
	{
		private static final long serialVersionUID = 20130404;

		@SuppressWarnings("unchecked")
		public OutlineTextField(String id, IModel<T> model, Class<?> type)
		{
			super(id, model, (Class<T>) type);
		}

		@Override
		protected void onComponentTag(final ComponentTag tag)
		{
			tag.setName("input");
			super.onComponentTag(tag);
		}

	}

	private final class OutlineLink extends AjaxLink<Void>
	{
		private static final long serialVersionUID = 20130404;
		private final Object result;

		public OutlineLink(final String id, final Object result, final String label)
		{
			super(id);

			this.result = result;
			this.setBody(Model.of(label));
			this.setEscapeModelStrings(false);
		}

		@Override
		public void onClick(final AjaxRequestTarget target)
		{
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		protected void onComponentTag(final ComponentTag tag)
		{
			tag.setName("a");
			super.onComponentTag(tag);
		}

	}

}
