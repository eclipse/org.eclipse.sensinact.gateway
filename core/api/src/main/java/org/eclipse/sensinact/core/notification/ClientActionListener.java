/*********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.core.notification;

/**
 * Used to register a session-based listener for resource data
 *
 * Events will be filtered based on the session's visibility of the resources
 */
public interface ClientActionListener {

    void notify(String topic, ResourceActionNotification event);

}
