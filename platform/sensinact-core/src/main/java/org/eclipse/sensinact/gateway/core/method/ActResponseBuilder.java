/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
package org.eclipse.sensinact.gateway.core.method;

import jakarta.json.JsonObject;

/**
 * Extended {@link AccessMethodResponseBuilder} dedicated to {@link ActMethod}
 * execution
 * 
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
@SuppressWarnings("serial")
public class ActResponseBuilder extends AccessMethodResponseBuilder<JsonObject, ActResponse> {
	/**
	 * @param uri
	 * @param parameters
	 */
	protected ActResponseBuilder(String uri, Object[] parameters) {
		super(uri, parameters);
	}

	/**
	 * @inheritDoc
	 *
	 * @see AccessMethodResponseBuilder#
	 *      createAccessMethodResponse(org.eclipse.sensinact.gateway.core.model.message.SnaMessage.Status)
	 */
	@Override
	public ActResponse createAccessMethodResponse(AccessMethodResponse.Status status) {
		ActResponse response = new ActResponse(super.getPath(), status);

		while (!super.isEmpty()) {
			response.addTriggered(super.pop());
		}
		return response;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.method.AccessMethodResponseBuilder#getComponentType()
	 */
	@Override
	public Class<JsonObject> getComponentType() {
		return JsonObject.class;
	}
}
