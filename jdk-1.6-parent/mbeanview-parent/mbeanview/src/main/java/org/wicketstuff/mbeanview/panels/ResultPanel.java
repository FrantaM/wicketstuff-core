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

import java.io.Serializable;
import java.lang.reflect.Array;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
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
	private static final int OUTLINE_LENGTH = 75;
	private final ModalWindow detailWindow;
	private final Object result;
	private final Class<?> resultType;
	private final boolean outline;
	private final boolean editable;

	public ResultPanel(final String id, final Object result, final Class<?> resultType)
	{
		this(id, result, resultType, false, false);
	}

	public ResultPanel(final String id, final Object result, Class<?> resultType, final boolean outline, final boolean editable)
	{
		super(id);

		this.result = result;
		this.resultType = resultType;
		this.outline = outline;
		this.editable = editable;

		this.add((this.detailWindow = new ModalWindow("detail")).setVisibilityAllowed(this.outline));
		this.add(this.resultContainer(result, resultType));
	}

	private Component resultContainer(final Object result, final Class<?> resultType)
	{
		return this.outline
				? this.resultContainerOutline(result, resultType)
				: this.resultContainerDialog(result, resultType);
	}

	private Component resultContainerOutline(final Object result, final Class<?> resultType)
	{
		final Fragment f = new Fragment(CONTAINER_ID, "result-text", this);

		if (result == null)
		{
			if (this.editable)
			{
				f.add(new OutlineTextField("result", Model.of(), resultType)
						.add(AttributeModifier.replace("placeholder", "null")));
			}
			else
			{
				f.add(new Label("result", "null"));
			}
		}
		else if (result instanceof CompositeData || result instanceof TabularData)
		{
			f.add(new OutlineLink("result", result.getClass().getName()));
		}
		else if (result instanceof Throwable)
		{
			f.add(new OutlineLink("result", ((Throwable) result).getLocalizedMessage()));
		}
		else if (result.getClass().isArray())
		{
			final String label = String.format(this.getLocale(), "%s items", Array.getLength(result)); // l10n
			f.add(new OutlineLink("result", label));
		}
		else
		{
			final String label = String.valueOf(result);
			final int length = Math.min(label.length(), OUTLINE_LENGTH);

			if (length < label.length())
			{
				final String linkLabel = String.format("%s&hellip;", label.substring(0, length));
				f.add(new OutlineLink("result", linkLabel));
			}
			else if (this.editable && result instanceof Serializable)
			{
				f.add(new OutlineTextField("result", Model.of((Serializable) result), resultType));
			}
			else
			{
				f.add(new Label("result", label));
			}
		}

		return f;
	}

	private Component resultContainerDialog(final Object result, final Class<?> resultType)
	{
		if (result == null)
		{
			return new Label(CONTAINER_ID, "null");
		}
		else if (result.getClass().isArray())
		{
			final Object[] array = new Object[Array.getLength(result)];
			for (int i = 0; i < array.length; ++i)
			{
				array[i] = Array.get(result, i);
			}

			if (array.length > 0)
			{
				final Fragment f = new Fragment(CONTAINER_ID, "result-list", this);
				f.add(new Loop("list", array.length)
				{
					private static final long serialVersionUID = 1L;

					@Override
					protected void populateItem(final LoopItem item)
					{
						item.add(new Label("result", String.valueOf(array[item.getIndex()])));
					}

				});

				return f;
			}
			else
			{
				return new Label(CONTAINER_ID, "<empty array>"); // l10n
			}
		}

		return new Label(CONTAINER_ID, String.valueOf(result));
	}

	private final class OutlineTextField extends Fragment
	{
		private static final long serialVersionUID = 20130404;

		public OutlineTextField(final String id, IModel<Serializable> model, Class<?> type)
		{
			super(id, "result-box", ResultPanel.this);

			final Form<?> form = new Form<Serializable>("form", model);
			this.add(form);

			@SuppressWarnings("unchecked")
			final Class<Serializable> serializable = (Class<Serializable>) type;
			form.add(new TextField<Serializable>("result", model, serializable));
			form.add(new AjaxButton("save")
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
				{
					super.onSubmit(target, form);
				}

				@Override
				protected void onError(final AjaxRequestTarget target, final Form<?> form)
				{
					super.onError(target, form);
				}

			});
		}

	}

	private final class OutlineLink extends AjaxLink<Void>
	{
		private static final long serialVersionUID = 20130404;

		public OutlineLink(final String id, final String label)
		{
			super(id);

			this.setBody(Model.of(label));
			this.setEscapeModelStrings(false);
		}

		@Override
		public void onClick(final AjaxRequestTarget target)
		{
			final ResultPanel rp = new ResultPanel(detailWindow.getContentId(), result, resultType, false, editable);
			detailWindow.setContent(rp);
			detailWindow.show(target);
		}

		@Override
		protected void onComponentTag(final ComponentTag tag)
		{
			tag.setName("a");
			super.onComponentTag(tag);
		}

	}

}
