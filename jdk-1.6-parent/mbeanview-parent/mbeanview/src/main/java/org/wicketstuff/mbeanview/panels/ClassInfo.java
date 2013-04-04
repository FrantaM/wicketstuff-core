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

import org.apache.wicket.core.util.lang.WicketObjects;

/**
 *
 * @author Franta Mejta
 * @sa.date 2013-04-04T17:37:47+0200
 */
final class ClassInfo
{
	private Class<?> classType;
	private String simpleName;
	private String name;

	public static ClassInfo of(final String jmxType)
	{
		final ClassInfo cn = new ClassInfo();
		cn.name = jmxType;
		cn.simpleName = jmxType;

		/* Why isn't void in AbstractClassResolver? */
		if (!"void".equals(jmxType))
		{
			final Class<?> clazz = WicketObjects.resolveClass(jmxType);
			if (clazz != null)
			{
				cn.simpleName = clazz.getSimpleName();
				cn.name = clazz.getName();
				cn.classType = clazz;

				if (clazz.isArray())
				{
					cn.name = String.format("%s[]", clazz.getComponentType().getName());
				}
			}
		}
		else
		{
			cn.classType = Void.class;
		}

		return cn;
	}

	public Class<?> getClassType()
	{
		return classType;
	}

	public String getSimpleName()
	{
		return simpleName;
	}

	public String getName()
	{
		return name;
	}

}
