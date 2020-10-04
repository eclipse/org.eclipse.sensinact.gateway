/*
* Copyright (c) 2020 Kentyou.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
*    Kentyou - initial API and implementation
 */
package org.eclipse.sensinact.gateway.core.remote;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Deque;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.common.execution.Executable;
import org.eclipse.sensinact.gateway.core.Session;
import org.eclipse.sensinact.gateway.core.message.Recipient;
import org.eclipse.sensinact.gateway.core.message.RemoteAgentCallback;
import org.eclipse.sensinact.gateway.core.message.SnaAgent;
import org.eclipse.sensinact.gateway.core.message.SnaConstants;
import org.eclipse.sensinact.gateway.core.message.SnaFilter;
import org.eclipse.sensinact.gateway.core.message.SnaMessage;
import org.eclipse.sensinact.gateway.core.message.SnaRemoteMessage;
import org.eclipse.sensinact.gateway.core.message.SnaRemoteMessageImpl;
import org.eclipse.sensinact.gateway.core.method.legacy.SubscribeResponse;
import org.eclipse.sensinact.gateway.util.UriUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link RemoteCore} implementation
 * 
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class RemoteSensiNact implements RemoteCore {
	
	// ********************************************************************//
	// NESTED DECLARATIONS //
	// ********************************************************************//

	/**
	 *
	 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
	 */
	final class SubscriptionReference {
		public final String publicKey;
		public final String serviceProviderId;
		public final String serviceId;
		public final String resourceId;

		SubscriptionReference(String publicKey, String serviceProviderId, String serviceId, String resourceId) {
			this.publicKey = publicKey;
			this.serviceProviderId = serviceProviderId;
			this.serviceId = serviceId;
			this.resourceId = resourceId;
		}
	}

	// ********************************************************************//
	// ABSTRACT DECLARATIONS //
	// ********************************************************************//

	// ********************************************************************//
	// STATIC DECLARATIONS //
	// ********************************************************************//

	public static final String NAMESPACE_PROP = "namespace";

	// ********************************************************************//
	// INSTANCE DECLARATIONS //
	// ********************************************************************//

	protected Mediator mediator;
	protected LocalEndpoint localEndpoint;
	protected RemoteEndpoint remoteEndpoint;

	protected Map<String, String> localAgents;
	protected ServiceRegistration<RemoteCore> registration;
	protected Map<String, SubscriptionReference> subscriptionIds;

	protected Deque<Executable<String, Void>> onConnectedCallback;
	protected Deque<Executable<String, Void>> onDisconnectedCallback;

	/**
	 * Constructor
	 * 
	 * @param mediator
	 *            the {@link Mediator} allowing to interact with the OSGi host
	 *            environment
	 * @param remoteEndpoint
	 *            the {@link RemoteEndpoint} the {@link RemoteCore} to be
	 *            instantiated will be attached to, in manner of communicating with
	 *            a remote instance of sensiNact
	 * @param localEndpoint
	 *            the {@link LocalEndpoint} the {@link RemoteCore} to be
	 *            instantiated will be attached to in manner of communicating with
	 *            the local instance of sensiNact
	 */
	public RemoteSensiNact(Mediator mediator, LocalEndpoint localEndpoint) {
		this.mediator = mediator;
		this.localAgents = new HashMap<String, String>();
		this.subscriptionIds = new HashMap<String, SubscriptionReference>();
		this.localEndpoint = localEndpoint;
		
		this.onConnectedCallback = new LinkedList<Executable<String, Void>>();
		this.onDisconnectedCallback = new LinkedList<Executable<String, Void>>();
		
		this.onConnectedCallback.add(new Executable<String,Void>(){
			@Override
			public Void execute(String namespace) throws Exception {
				RemoteSensiNact.this.propagateRemoteConnectionStatus(true, namespace);
				return null;
			}
		});
		this.onDisconnectedCallback.add(new Executable<String,Void>(){
			@Override
			public Void execute(String namespace) throws Exception {
				RemoteSensiNact.this.propagateRemoteConnectionStatus(false, namespace);
				return null;
			}
		});
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#onConnected(org.eclipse.sensinact.gateway.common.execution.Executable)
	 */
	@Override
	public void onConnected(Collection<Executable<String, Void>> onConnectedCallbacks) {
		if (onConnectedCallbacks == null) {
			return;
		}
		this.onConnectedCallback.addAll(onConnectedCallbacks);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#onDisconnected(org.eclipse.sensinact.gateway.common.execution.Executable)
	 */
	@Override
	public void onDisconnected(Collection<Executable<String, Void>> onDisconnectedCallbacks) {
		if (onDisconnectedCallbacks == null) {
			return;
		}
		if (this.onDisconnectedCallback == null) {
		}
		this.onDisconnectedCallback.addAll(onDisconnectedCallbacks);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#open(org.eclipse.sensinact.gateway.core.remote.RemoteEndpoint)
	 */
	@Override
	public void open(RemoteEndpoint remoteEndpoint) {
		if (remoteEndpoint == null) {
			throw new NullPointerException("A remote endpoint is needed");
		}
		this.remoteEndpoint = remoteEndpoint;
		this.remoteEndpoint.open(this);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#connect(java.lang.String)
	 */
	@Override
	public void connect(final String namespace) {
		synchronized(this) {
			if (namespace == null) {
				mediator.debug("Unable to register because of null namespace");
				return;
			}
			this.registration = AccessController.<ServiceRegistration<RemoteCore>>doPrivileged(
				new PrivilegedAction<ServiceRegistration<RemoteCore>>() {
					@Override
					public ServiceRegistration<RemoteCore> run() {
						Dictionary<String, Object> props = new Hashtable<String, Object>();
						props.put(NAMESPACE_PROP, namespace);

						ServiceRegistration<RemoteCore> reg = mediator.getContext().registerService(
							RemoteCore.class, RemoteSensiNact.this, props);
						mediator.debug("RemoteCore '%s' registration done", namespace);
						return reg;
					}
				});
			if (this.registration == null) {
				mediator.debug("Registration error");
				return;
			}
			if (this.onConnectedCallback != null) {
				Iterator<Executable<String, Void>> it = this.onConnectedCallback.iterator();
				while (it.hasNext()) {
					try {
						it.next().execute(namespace);
					} catch (Exception e) {
						mediator.error(e);
					}
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#close()
	 */
	@Override
	public void close() {
		this.disconnect();
		synchronized (this.subscriptionIds) {
			this.subscriptionIds.clear();
		}
		this.remoteEndpoint.close();
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#disconnect()
	 */
	@Override
	public void disconnect() {
		synchronized(this) {
			final String namespace = this.remoteEndpoint.namespace();
			if (this.onDisconnectedCallback!= null && !this.onDisconnectedCallback.isEmpty() && namespace!=null) {
				Iterator<Executable<String, Void>> it = this.onDisconnectedCallback.iterator();
				while (it.hasNext()) {
					try {
						it.next().execute(namespace);
					} catch (Exception e) {
						mediator.error(e);
					}
				}
			}
			if (this.registration != null) {
				AccessController.<Void>doPrivileged(new PrivilegedAction<Void>() {
					@Override
					public Void run() {
						try {
							RemoteSensiNact.this.registration.unregister();
	
						} catch (IllegalStateException e) {
							RemoteSensiNact.this.mediator.error(e);
	
						} finally {
							RemoteSensiNact.this.registration = null;
						}
						return null;
					}
				});
			}
			if (!this.subscriptionIds.isEmpty()) {
				String[] keys = null;
				synchronized (this.subscriptionIds) {
					keys = new String[this.subscriptionIds.size()];
					this.subscriptionIds.keySet().toArray(keys);				
					int index = 0;
					int length = keys == null ? 0 : keys.length;
					for (; index < length; index++) {
						SubscriptionReference ref = this.subscriptionIds.remove(keys[index]);
						if (ref == null) {
							continue;
						}
						JSONObject response = this.localEndpoint.unsubscribe(ref.publicKey, 
							ref.serviceProviderId, ref.serviceId, ref.resourceId, keys[index]);
						mediator.debug(response.toString());
					}
				}
			}
			this.localEndpoint.close();
			this.localAgents.clear();
		}
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#endpoint()
	 */
	@Override
	public RemoteEndpoint endpoint() {
		return this.remoteEndpoint;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getAll(java.lang.String)
	 */
	@Override
	public String getAll(String publicKey) {
		return this.getAll(publicKey, null);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getAll(java.lang.String,java.lang.String)
	 */
	@Override
	public String getAll(String publicKey, String filter) {
		return this.localEndpoint.getAll(publicKey, filter);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getProviders(java.lang.String)
	 */
	@Override
	public String getProviders(String publicKey) {
		return this.localEndpoint.getProviders(publicKey);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getProvider(java.lang.String, java.lang.String)
	 */
	@Override
	public String getProvider(String publicKey, String serviceProviderId) {
		return this.localEndpoint.getProvider(publicKey, serviceProviderId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getServices(java.lang.String, java.lang.String)
	 */
	@Override
	public String getServices(String publicKey, String serviceProviderId) {
		return this.localEndpoint.getServices(publicKey, serviceProviderId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getService(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String getService(String publicKey, String serviceProviderId, String serviceId) {
		return this.localEndpoint.getService(publicKey, serviceProviderId, serviceId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * getResources(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String getResources(String publicKey, String serviceProviderId, String serviceId) {
		return this.localEndpoint.getResources(publicKey, serviceProviderId, serviceId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * getResource(java.lang.String,java.lang.String,java.lang.String,java.lang.String)
	 */
	@Override
	public String getResource(String publicKey, String serviceProviderId, String serviceId, String resourceId) {
		return this.localEndpoint.getResource(publicKey, serviceProviderId, serviceId, resourceId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * get(java.lang.String,java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public JSONObject get(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			String attributeId) {
		return this.localEndpoint.get(publicKey, serviceProviderId, serviceId, resourceId, attributeId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * set(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.Object)
	 */
	@Override
	public JSONObject set(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			String attributeId, Object parameter) {
		return this.localEndpoint.set(publicKey, serviceProviderId, serviceId, resourceId, attributeId, parameter);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint# 
	 * act(java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.Object[])
	 */
	@Override
	public JSONObject act(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			Object[] parameters) {
		return this.localEndpoint.act(publicKey, serviceProviderId, serviceId, resourceId, parameters);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#
	 * subscribe(java.lang.String,java.lang.String,java.lang.String,java.lang.String,org.json.JSONArray)
	 */
	@Override
	public JSONObject subscribe(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			JSONArray conditions) {
		JSONObject response = this.subscribe(publicKey, serviceProviderId, serviceId, resourceId, this.remoteEndpoint,
				conditions);
		return response;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * subscribe(java.lang.String,java.lang.String,java.lang.String,java.lang.String,org.eclipse.sensinact.gateway.core.message.Recipient,org.json.JSONArray)
	 */
	@Override
	public JSONObject subscribe(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			Recipient recipient, JSONArray conditions) {
		JSONObject response = this.localEndpoint.subscribe(publicKey, serviceProviderId, serviceId, resourceId,
				recipient, conditions);

		if (response.optInt("statusCode") == 200) {
			try {
				String subscriptionId = response.getJSONObject("response").getString("subscriptionId");

				this.subscriptionIds.put(subscriptionId,
						new SubscriptionReference(publicKey, serviceProviderId, serviceId, resourceId));

			} catch (JSONException e) {
				mediator.error(e);
			}
		}
		return response;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * unsubscribe(java.lang.String, java.lang.String, java.lang.String,java.lang.String, java.lang.String)
	 */
	@Override
	public JSONObject unsubscribe(String publicKey, String serviceProviderId, String serviceId, String resourceId,
			String subscriptionId) {
		this.subscriptionIds.remove(subscriptionId);

		JSONObject response = this.localEndpoint.unsubscribe(publicKey, serviceProviderId, serviceId, resourceId,
				subscriptionId);
		return response;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#isAccessible(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isAccessible(String publicKey, String path) {
		return this.localEndpoint.isAccessible(publicKey, path);
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#namespace()
	 */
	@Override
	public String namespace() {
		return this.localEndpoint.localNamespace();
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#localID()
	 */
	@Override
	public int localID() {
		return this.localEndpoint.localID();
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#
	 * registerAgent(java.lang.String,org.eclipse.sensinact.gateway.core.message.SnaFilter,java.lang.String)
	 */
	@Override
	public void registerAgent(final String remoteAgentId, final SnaFilter filter, final String publicKey) {		
		if(this.localAgents.get(remoteAgentId) != null) {
			mediator.warn("the remote agent already exists");
			return;
		}
		Session session = this.localEndpoint.getSession(publicKey);
		RemoteAgentCallback callback = new RemoteAgentCallback(remoteAgentId, this);
		SubscribeResponse resp = session.registerSessionAgent(callback, filter);
		JSONObject registration = resp.getResponse();
		String localAgentId = null;

		if (!JSONObject.NULL.equals(registration) && (localAgentId = (String) 
			registration.opt("subscriptionId")) != null) {
			this.localAgents.put(remoteAgentId, localAgentId);
		}
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#unregisterAgent(java.lang.String)
	 */
	@Override
	public void unregisterAgent(String remoteAgentId) {
		String localAgentId = this.localAgents.remove(remoteAgentId);
		if (localAgentId == null) {
			return;
		}
		this.localEndpoint.unregisterAgent(localAgentId);
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#
	 * dispatch(java.lang.String,org.eclipse.sensinact.gateway.core.message.SnaMessage)
	 */
	@Override
	public void dispatch(String agentId, final SnaMessage<?> message) {
		this.callAgent(agentId, true, new Executable<SnaAgent, Void>() {
			@Override
			public Void execute(SnaAgent agent) throws Exception {
				if (agent != null) {
					agent.register(message);
				}
				return null;
			}
		});
	}

	/**
	 * @inheritDoc
	 * 
	 * @see org.eclipse.sensinact.gateway.core.remote.RemoteCore#closeSession(java.lang.String)
	 */
	@Override
	public void closeSession(String publicKey) {
		this.localEndpoint.closeSession(publicKey);
	}


	private final void callAgent(final String agentId, final boolean local, 
			final Executable<SnaAgent, Void> executable) {
		AccessController.<Void>doPrivileged(new PrivilegedAction<Void>() {
			@Override
			public Void run() {
			    RemoteSensiNact.this.mediator.callServices(SnaAgent.class,
				new StringBuilder().append("(org.eclipse.sensinact.gateway.agent.id="
				).append(agentId).append(")").toString(), executable);
			    return null;
			}
		});
	}
	
	private final void propagateRemoteConnectionStatus(boolean connected, String namespace) {
		SnaRemoteMessageImpl message = new SnaRemoteMessageImpl(mediator, UriUtils.ROOT, 
			connected?SnaRemoteMessage.Remote.CONNECTED:SnaRemoteMessage.Remote.DISCONNECTED);
		message.setNotification(new JSONObject().put(SnaConstants.REMOTE, 
			connected?SnaRemoteMessage.Remote.CONNECTED.name():SnaRemoteMessage.Remote.DISCONNECTED.name())
				.put(SnaConstants.NAMESPACE, namespace));
		dispatch("*", message);
	}
}
