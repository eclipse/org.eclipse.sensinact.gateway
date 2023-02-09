/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.sensinact.prototype.command;

import java.util.Map;

/**
 * The digital twin of a Service instance
 */
public interface SensinactService extends CommandScoped {

    /**
     * The service name. Defined by the model
     *
     * @return
     */
    String getName();

    /**
     * The provider instance that owns this service instance
     *
     * @return
     */
    SensinactProvider getProvider();

    /**
     * The resource instance that owns this service instance
     *
     * @return
     */
    Map<String, ? extends SensinactResource> getResources();
}