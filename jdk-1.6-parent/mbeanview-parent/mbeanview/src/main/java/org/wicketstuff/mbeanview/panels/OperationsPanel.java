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

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ObjectName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.util.lang.Classes;
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

	public OperationsPanel(final String id, final MBeanOperationInfo[] operations)
	{
		super(id);

		this.add(this.newOperationsList("operation", operations));
	}

	private RepeatingView newOperationsList(final String id, final MBeanOperationInfo[] operations)
	{
		final RepeatingView view = new RepeatingView(id);
		for (final MBeanOperationInfo operation : operations)
		{
			final WebMarkupContainer row = new WebMarkupContainer(view.newChildId());

			final ClassName returnType = this.className(operation.getReturnType());
			row.add(new Label("returnType", returnType.simpleName)
					.add(AttributeModifier.replace("title", returnType.name)));
			row.add(new Label("methodName", operation.getName()));

			final RepeatingView parameters = new RepeatingView("parameter");
			final MBeanParameterInfo[] mbeanParameters = operation.getSignature();
			for (int i = 0; i < mbeanParameters.length; ++i)
			{
				final MBeanParameterInfo mbeanParameter = mbeanParameters[i];
				final WebMarkupContainer parameterCont = new WebMarkupContainer(parameters.newChildId());
				parameterCont.add(new Label("parameterName", mbeanParameter.getName()));
				parameterCont.add(this.editorFor("parameterEditor", mbeanParameter));
				parameterCont.add(new WebMarkupContainer("separator").setVisibilityAllowed(i > 0));

				parameters.add(parameterCont);
			}

			row.add(parameters);
			row.add(new Label("description", operation.getDescription())
					.setVisibilityAllowed(!operation.getName().equalsIgnoreCase(operation.getDescription())));

			view.add(row);
		}

		return view;
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

			if (clazz.isArray()) {
				cn.name = String.format("%s[]", clazz.getComponentType().getName());
			}
		}
		catch (final ClassNotFoundException ex)
		{
			log.debug("Cannot find class [{}].", jmxType, ex);
		}

		return cn;
	}

	private WebMarkupContainer editorFor(final String id, final MBeanParameterInfo parameter)
	{
		final TextField<?> input = new TextField<String>("parameterValue");
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

}
