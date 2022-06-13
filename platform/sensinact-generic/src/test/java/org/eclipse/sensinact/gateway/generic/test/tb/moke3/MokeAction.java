/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.sensinact.gateway.generic.test.tb.moke3;

import org.eclipse.sensinact.gateway.generic.ExtModelInstance;
import org.eclipse.sensinact.gateway.generic.ExtResourceConfig;
import org.eclipse.sensinact.gateway.generic.ExtResourceImpl;
import org.eclipse.sensinact.gateway.generic.ExtServiceImpl;
import org.eclipse.sensinact.gateway.generic.annotation.Act;
import org.eclipse.sensinact.gateway.util.json.JsonProviderFactory;

import jakarta.json.JsonObject;

/**
 *
 */
public class MokeAction extends ExtResourceImpl {
    /**
     * @param mediator
     * @param resourceConfig
     * @param service
     */
    protected MokeAction(ExtModelInstance<?> snaModelInstance, ExtResourceConfig resourceConfig, ExtServiceImpl service) {
        super(snaModelInstance, resourceConfig, service);
    }

    @Act
    JsonObject act(String number, String message) {

        JsonObject jsonObject = JsonProviderFactory.getProvider().createObjectBuilder()
    			.add("message", number + " called : " + message).build();

        System.out.println("-----------------------------");
        System.out.println(jsonObject);
        System.out.println("-----------------------------");

        return jsonObject;
    }

    @Act
    public JsonObject act() {
        JsonObject jsonObject = JsonProviderFactory.getProvider().createObjectBuilder()
    			.add("message", "empty call").build();
        System.out.println("-----------------------------");
        System.out.println(jsonObject);
        System.out.println("-----------------------------");

        return jsonObject;
    }
}
