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
package org.eclipse.sensinact.core.emf.model;

import java.time.Instant;

import org.eclipse.sensinact.core.model.ResourceBuilder;
import org.eclipse.sensinact.core.model.ServiceBuilder;

/**
 * A builder for programmatically registering models
 */
public interface EMFServiceBuilder<T> extends ServiceBuilder<T> {

    EMFServiceBuilder<T> exclusivelyOwned(boolean exclusive);

    EMFServiceBuilder<T> withAutoDeletion(boolean autoDelete);

    EMFServiceBuilder<T> withCreationTime(Instant creationTime);

    ResourceBuilder<ServiceBuilder<T>, Object> withResource(String name);

    T build();

    void buildAll();
}
