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
package org.eclipse.sensinact.gateway.nthbnd.rest.internal.http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundMediator;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundRequest;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.RegisteringResponse;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.RestAccessConstants;
import org.eclipse.sensinact.gateway.util.IOUtils;
import org.json.JSONObject;

/**
 * This class is the REST interface between each others classes
 * that perform a task and jersey
 */
@SuppressWarnings("serial")
@WebServlet(asyncSupported = true)
public class HttpRegisteringEndpoint extends HttpServlet {
    private NorthboundMediator mediator;

    /**
     * Constructor
     */
    public HttpRegisteringEndpoint(NorthboundMediator mediator) {
        this.mediator = mediator;
    }

    /**
     * @inheritDoc
     * @see javax.servlet.http.HttpServlet#
     * doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        this.doExecute(request, response);
    }

    /**
     * @param request
     * @param response
     * @throws IOException
     */
    private final void doExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        final AsyncContext asyncContext;
        if (request.isAsyncStarted()) {
            asyncContext = request.getAsyncContext();

        } else {
            asyncContext = request.startAsync(request, response);
        }
        response.getOutputStream().setWriteListener(new WriteListener() {
            /**
             * @inheritDoc
             *
             * @see javax.servlet.WriteListener#onWritePossible()
             */
            @Override
            public void onWritePossible() throws IOException {
                HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                try {
                    String queryString = request.getQueryString();
                    if(queryString == null) {
                    	response.sendError(400, "'create' or 'renew' request parameter expected");
                    }
                    Map<String,List<String>> map = NorthboundRequest.processRequestQuery(queryString);
                    String query = null;
                    List<String> list = map.get("request");
                    if(list!= null){
                    	query = list.get(list.size()-1);
                    }else {
                    	query = map.get("create")!=null?"create":(map.get("renew")!=null?"renew":null);
                    }
                    if(query == null) {
                    	response.sendError(400, "'create' or 'renew' request parameter expected");
                    }                    
                    byte[] content = IOUtils.read(request.getInputStream(),false);
                    JSONObject jcontent = new JSONObject(new String(content));          

                    RegisteringResponse registeringResponse = null;
                    
                    switch(query) {
                        case "create":
                        	String login = (String) jcontent.opt("login");
                        	String password= (String) jcontent.opt("password");
                        	String account= (String) jcontent.opt("account");
                        	String accountType= (String) jcontent.opt("accountType");
                            registeringResponse = mediator.getAccessingEndpoint().registeringEndpoint(login, password, account, accountType);
                             break;
                        case "renew":
                        	 account= (String) jcontent.opt("account");
                            registeringResponse = mediator.getAccessingEndpoint().passwordRenewingEndpoint(account);
                             break;
						default:
                        	response.sendError(400, "'create' or 'renew' request parameter expected");
                    }
                    byte[] resultBytes = registeringResponse.getJSON().getBytes();
                    response.setContentType(RestAccessConstants.JSON_CONTENT_TYPE);
                    response.setContentLength(resultBytes.length);
                    response.setBufferSize(resultBytes.length);

                    ServletOutputStream output = response.getOutputStream();
                    output.write(resultBytes);

                    response.setStatus(200);

                } catch (ClassCastException e) {
                    mediator.error(e);
                    response.sendError(400, "Invalid parameters type");

                } catch (Exception e) {
                    mediator.error(e);
                    response.sendError(520, "Internal server error");

                } finally {
                    if (request.isAsyncStarted()) {
                        asyncContext.complete();
                    }
                }
            }

            /**
             * @inheritDoc
             *
             * @see javax.servlet.WriteListener#onError(java.lang.Throwable)
             */
            @Override
            public void onError(Throwable t) {
                mediator.error(t);
            }
        });
    }
}
