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
package org.eclipse.sensinact.prototype.model;

import java.io.InputStream;
import java.util.Map;

/**
 * The sensiNact interface used to create and discover with the models
 */
public interface ModelManager {

    ProviderBuilder createProvider(String provider);

    ServiceBuilder createService(String provider, String service);

    ResourceBuilder<?> createResource(String provider, String service, String resource);

    void deleteProvider(String provider);

    void deleteService(String provider, String service);

    void deleteResource(String provider, String service, String resource);

    void registerModel(String model);

    void registerModel(InputStream model);

    Map<String, Provider> getProviders();

    Map<String, Service> getServices(String provider);

    Map<String, Resource> getResources(String provider, String service);

}