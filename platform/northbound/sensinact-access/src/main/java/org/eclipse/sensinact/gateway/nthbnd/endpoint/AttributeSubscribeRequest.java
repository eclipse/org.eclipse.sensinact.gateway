/*
 * Copyright (c) 2017 CEA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    CEA - initial API and implementation
 */
package org.eclipse.sensinact.gateway.nthbnd.endpoint;

import org.json.JSONArray;

public class AttributeSubscribeRequest extends AttributeRequest {
    private NorthboundRecipient recipient;
    private JSONArray conditions;
	private String policy;

    /**
     * @param mediator
     * @param serviceProvider
     * @param service
     * @param resource
     * @param attribute
     * @param recipient
     */
    public AttributeSubscribeRequest(NorthboundMediator mediator, String requestIdentifier, 
    		String serviceProvider, String service, String resource, String attribute, 
    		NorthboundRecipient recipient, JSONArray conditions, String policy) {
    	
        super(mediator, requestIdentifier, serviceProvider, service, resource, attribute);
        this.recipient = recipient;
        this.conditions = conditions;
        this.policy = policy;
        
        if (this.recipient == null) {
            throw new NullPointerException("Recipient missing");
        }
    }

    /**
     * @inheritDoc
     * @see ServiceProvidersRequest#getExecutionArguments()
     */
    @Override
    protected Argument[] getExecutionArguments() {
        Argument[] superArguments = super.getExecutionArguments();
        int length = superArguments == null ? 0 : superArguments.length;
        Argument[] arguments = new Argument[length + 3];
        if (length > 0) {
            System.arraycopy(superArguments, 0, arguments, 0, length);
        }
        arguments[length] = new Argument(NorthboundRecipient.class, this.recipient);
        arguments[length + 1] = new Argument(JSONArray.class, this.conditions);
        arguments[length + 2] = new Argument(String.class, this.policy);
        return arguments;
    }

    /**
     * @inheritDoc
     * @see ResourceRequest#getMethod()
     */
    @Override
    protected String getMethod() {
        return "subscribe";
    }
}
