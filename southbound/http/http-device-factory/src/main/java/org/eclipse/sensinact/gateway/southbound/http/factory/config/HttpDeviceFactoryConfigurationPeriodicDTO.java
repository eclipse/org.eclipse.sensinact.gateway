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
package org.eclipse.sensinact.gateway.southbound.http.factory.config;

import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Root configuration of a device factory
 */
public class HttpDeviceFactoryConfigurationPeriodicDTO extends HttpDeviceFactoryConfigurationTaskDTO {

    /**
     * Refresh period in seconds (5 minutes by default)
     */
    public int period = 300;

    /**
     * Refresh period unit
     */
    @JsonProperty("period.unit")
    public ChronoUnit periodUnit = ChronoUnit.SECONDS;
}
