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
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-03T11:35:54+0200
 */
public class OperationsPanel extends GenericPanel<ObjectName>
{
	private static final long serialVersionUID = 20130403;
	private static final Logger log = LoggerFactory.getLogger(OperationsPanel.class);
	private static final MetaDataKey<Integer> PARAMETER_INDEX = new MetaDataKey<Integer>()
	{
		private static final long serialVersionUID = 1L;
	};
	private final ModalWindow operationOutput;

	public OperationsPanel(final String id, final MBeanOperationInfo[] operations)
	{
		super(id);

		this.add(this.newOperationsList("operation", operations));
		this.add(this.operationOutput = new ModalWindow("output"));
	}

	private RepeatingView newOperationsList(final String id, final MBeanOperationInfo[] operations)
	{
		final RepeatingView view = new RepeatingView(id);
		for (final MBeanOperationInfo operation : operations)
		{
			final ClassName returnType = this.className(operation.getReturnType());
			final Component returnTypeLabel = new Label("returnType", returnType.simpleName)
					.add(AttributeModifier.replace("title", returnType.name));

			final WebMarkupContainer descriptionRow = new WebMarkupContainer("description-row");
			descriptionRow.setVisibilityAllowed(!operation.getName().equalsIgnoreCase(operation.getDescription()));
			descriptionRow.add(new Label("description", operation.getDescription()));

			final WebMarkupContainer row = new WebMarkupContainer(view.newChildId());
			row.add(returnTypeLabel);
			row.add(this.newMethodCallForm("methodCall", operation));
			row.add(descriptionRow);

			view.add(row);
		}

		return view;
	}

	private WebMarkupContainer newMethodCallForm(final String id, final MBeanOperationInfo operation)
	{
		final Form<?> form = new Form<Void>(id);
		form.add(this.newMethodCallButton("method", operation));

		final RepeatingView parameters = new RepeatingView("parameter");
		final MBeanParameterInfo[] mbeanParameters = operation.getSignature();
		for (int i = 0; i < mbeanParameters.length; ++i)
		{
			final MBeanParameterInfo mbeanParameter = mbeanParameters[i];
			final WebMarkupContainer parameterCont = new WebMarkupContainer(parameters.newChildId());
			parameterCont.add(new Label("parameterName", mbeanParameter.getName()));
			parameterCont.add(this.editorFor("parameterEditor", mbeanParameter, i));
			parameterCont.add(new WebMarkupContainer("separator").setVisibilityAllowed(i > 0));

			parameters.add(parameterCont);
		}

		form.add(parameters);

		return form;
	}

	private WebMarkupContainer newMethodCallButton(final String id, final MBeanOperationInfo operation)
	{
		final AjaxButton button = new IndicatingAjaxButton(id)
		{
			private static final long serialVersionUID = 1L;

			private Object[] collectParameters(final Form<?> form)
			{
				final SortedMap<Integer, Object> parameters = new TreeMap<Integer, Object>();
				form.visitFormComponents(new IVisitor<FormComponent<?>, Void>()
				{
					@Override
					public void component(final FormComponent<?> object, final IVisit<Void> visit)
					{
						final Integer index = object.getMetaData(PARAMETER_INDEX);
						if (index != null)
						{
							assert !parameters.containsKey(index) : "Duplicate parameter index [" + index + "].";
							parameters.put(index, object.getConvertedInput());
						}
					}

				});

				return parameters.values().toArray();
			}

			@Override
			protected void onSubmit(final AjaxRequestTarget target, final Form<?> form)
			{
				final InvokeOperationEvent event = new InvokeOperationEvent(operation, this.collectParameters(form));
				this.send(this, Broadcast.BUBBLE, event);

				final StringBuilder result = new StringBuilder();
				if (event.getException() != null)
				{
					result.append("Problem invoking ");
					result.append(operation.getName());
					result.append(": ");
					result.append(event.getException());
				}
				else
				{
					result.append("Operation invoked successfully.");
					if (event.getResult() != null)
					{
						result.append(" Result: ");
						result.append(event.getResult());
					}
				}

				operationOutput.setContent(new Label(operationOutput.getContentId(), result.toString()));
				operationOutput.show(target);
			}

			@Override
			protected void onError(final AjaxRequestTarget target, final Form<?> form)
			{
				operationOutput.setContent(new FeedbackPanel(operationOutput.getContentId()));
				operationOutput.show(target);
			}

		};

		button.add(new Label("methodName", operation.getName()));
		return button;
	}

	private ClassName className(final String jmxType)
	{
		final ClassName cn = new ClassName();
		cn.name = jmxType;
		cn.simpleName = jmxType;

		try
		{
			final Class<?> clazz = Class.forName(jmxType, false, this.getClass().getClassLoader());
			cn.simpleName = Classes.simpleName(clazz);
			cn.name = clazz.getName();

			if (clazz.isArray())
			{
				cn.name = String.format("%s[]", clazz.getComponentType().getName());
			}
		}
		catch (final ClassNotFoundException ex)
		{
			log.debug("Cannot find class [{}].", jmxType, ex);
		}

		return cn;
	}

	private WebMarkupContainer editorFor(final String id, final MBeanParameterInfo parameter, final Integer index)
	{
		final TextField<?> input = new RequiredTextField<String>("parameterValue", Model.<String>of());
		input.setMetaData(PARAMETER_INDEX, index);
		input.add(AttributeModifier.replace("placeholder", this.className(parameter.getType()).simpleName));

		final Fragment fragment = new Fragment(id, "editor-text", this);
		fragment.add(input);

		return fragment;
	}

	private static final class ClassName
	{
		private String simpleName;
		private String name;
	}

	public static final class InvokeOperationEvent implements Serializable
	{
		private static final long serialVersionUID = 20130403;
		private final MBeanOperationInfo operation;
		private final Object[] parameters;
		private Object result;
		private Exception exception;

		public InvokeOperationEvent(final MBeanOperationInfo operation, final Object... parameters)
		{
			this.operation = operation;
			this.parameters = parameters;
		}

		public MBeanOperationInfo getOperation()
		{
			return operation;
		}

		public Object[] getParameters()
		{
			return parameters;
		}

		public Object getResult()
		{
			return result;
		}

		public void setResult(Object result)
		{
			this.result = result;
		}

		public Exception getException()
		{
			return exception;
		}

		public void setException(Exception exception)
		{
			this.exception = exception;
		}

	}

}
