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
package org.eclipse.sensinact.gateway.nthbnd.rest.internal.ws;

import java.io.IOException;
import java.util.List;

import org.eclipse.sensinact.gateway.core.ResultHolder;
import org.eclipse.sensinact.gateway.core.security.Authentication;
import org.eclipse.sensinact.gateway.core.security.AuthenticationToken;
import org.eclipse.sensinact.gateway.core.security.InvalidCredentialException;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundAccess;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundEndpoint;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundEndpoints;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundMediator;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRequest;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRequestBuilder;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.format.StringResponseFormat;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.RestAccessConstants;
import org.json.JSONObject;

/**
 * Extended {@link NorthbundAccess} dedicated to websocket connections
 * 
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class WsRestAccess extends NorthboundAccess<JSONObject, WsRestAccessRequest>
{
	/**
	 * The {@link WebSocketWrapper} held by this WsRestAccess 
	 */
	private WebSocketWrapper socket;
	private NorthboundEndpoint endpoint;

	/**
	 * Constructor
	 * 
	 * @param socket the {@link WebSocketWrapper} held by 
	 * the WsRestAccess to be instantiated
	 * 
	 * @throws IOException 
	 * @throws InvalidCredentialException 
	 */
	public WsRestAccess(WsRestAccessRequest request, WebSocketWrapper socket) 
		throws IOException, InvalidCredentialException
	{
		super(request);
		this.socket = socket;
		
		Authentication<?> authentication = request.getAuthentication();
		if(authentication == null)
		{
			this.endpoint = request.getMediator(
				).getNorthboundEndpoints().getEndpoint();
			
		} else if(AuthenticationToken.class.isAssignableFrom(
				authentication.getClass()))
		{
			this.endpoint = request.getMediator(
			).getNorthboundEndpoints().getEndpoint(
				(AuthenticationToken)authentication);
		} else
		{
			throw new InvalidCredentialException(
				"Authentication token was expected");
		}
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.AbstractNorthboundRequestHandler#respond(org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRequestBuilder)
	 */
	@Override
	protected boolean respond(NorthboundMediator mediator, 
			NorthboundRequestBuilder<JSONObject> builder) 
			throws IOException 
	{
		NorthboundRequest nthbndRequest = builder.build();
		if(nthbndRequest == null)
		{
			sendError(500, "Internal server error");
			return false;
		}
		ResultHolder<?> cap = this.endpoint.execute(nthbndRequest);
		String result = new StringResponseFormat().format(cap.getResult());
		if(result == null)
		{
			sendError(500, "Internal server error");
			return false;
		}
		byte[] resultBytes;
		List<String> acceptEncoding = super.request.getQueryMap(
				).get("Accept-Encoding");
        if(acceptEncoding != null && acceptEncoding.contains("gzip")) 
        {
            resultBytes = NorthboundAccess.compress(result);
    		this.socket.send(resultBytes);
            
        }  else
        {
        	resultBytes = result.getBytes("UTF-8");
    		this.socket.send(new String(resultBytes));
        }
		return true;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.nthbnd.endpoint.AbstractNorthboundRequestHandler#sendError(int, java.lang.String)
	 */
	@Override
	protected void sendError(int i, String string) throws IOException 
	{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("statusCode", i);
		jsonObject.put("message", string);
		this.socket.send(new String(jsonObject.toString().getBytes("UTF-8")));
	}

}
