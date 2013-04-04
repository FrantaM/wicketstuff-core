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

import java.io.Serializable;

import javax.management.MBeanServerConnection;

/**
 * @author Franta Mejta
 * @sa.date 2013-03-29T16:14:56+0100
 */
public interface IMBeanServerConnectionProvider extends Serializable
{
	/**
	 * Returns connection to MBean server.
	 * Must not return {@code null}.
	 *
	 * @return Connection to MBean server.
	 */
	MBeanServerConnection get();

}
