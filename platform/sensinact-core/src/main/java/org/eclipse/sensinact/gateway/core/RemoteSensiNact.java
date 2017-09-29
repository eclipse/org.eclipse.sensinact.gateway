/*
 * #%L
 * sensiNact IoT Gateway - HTTP REST Access
 * %%
 * Copyright (C) 2015 CEA
 * %%
 * sensiNact - 2015
 * 
 * CEA - Commissariat a l'energie atomique et aux energies alternatives
 * 17 rue des Martyrs
 * 38054 Grenoble
 * France
 * 
 * Copyright(c) CEA
 * All Rights Reserved
 * #L%
 */

package org.eclipse.sensinact.gateway.core;


import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.common.execution.Executable;
import org.eclipse.sensinact.gateway.core.message.AbstractSnaAgentCallback;
import org.eclipse.sensinact.gateway.core.message.Recipient;
import org.eclipse.sensinact.gateway.core.message.SnaAgent;
import org.eclipse.sensinact.gateway.core.message.SnaAgentImpl;
import org.eclipse.sensinact.gateway.core.message.SnaErrorMessageImpl;
import org.eclipse.sensinact.gateway.core.message.SnaFilter;
import org.eclipse.sensinact.gateway.core.message.SnaLifecycleMessageImpl;
import org.eclipse.sensinact.gateway.core.message.SnaMessage;
import org.eclipse.sensinact.gateway.core.message.SnaResponseMessage;
import org.eclipse.sensinact.gateway.core.message.SnaUpdateMessageImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link RemoteCore} implementation
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class RemoteSensiNact implements RemoteCore
{	
	//********************************************************************//
	//						NESTED DECLARATIONS		    				  //
	//********************************************************************//
	
	final class RemoteSensiNactCallback extends AbstractSnaAgentCallback
	{	
		private String agentId;

		RemoteSensiNactCallback(String agentId)
		{
			this.agentId = agentId;
		}
		
		/**
		 * @inheritDoc
		 *
		 * @see org.eclipse.sensinact.gateway.core.message.SnaAgentCallback#
		 * doHandle(org.eclipse.sensinact.gateway.core.message.SnaLifecycleMessageImpl)
		 */
		@Override
		public void doHandle(SnaLifecycleMessageImpl message)
		{
			RemoteSensiNact.this.endpoint().dispatch(this.agentId, message);
		}

		/**
		 * @inheritDoc
		 *
		 * @see org.eclipse.sensinact.gateway.core.message.SnaAgentCallback#
		 * doHandle(org.eclipse.sensinact.gateway.core.message.SnaUpdateMessageImpl)
		 */
		@Override
		public void doHandle(SnaUpdateMessageImpl message)
		{
			RemoteSensiNact.this.endpoint().dispatch(this.agentId, message);
		}

		/**
		 * @inheritDoc
		 *
		 * @see org.eclipse.sensinact.gateway.core.message.SnaAgentCallback#
		 * doHandle(org.eclipse.sensinact.gateway.core.message.SnaErrorMessageImpl)
		 */
		@Override
		public void doHandle(SnaErrorMessageImpl message)
		{
			RemoteSensiNact.this.endpoint().dispatch(this.agentId, message);
		}

		/**
		 * @inheritDoc
		 *
		 * @see org.eclipse.sensinact.gateway.core.message.SnaAgentCallback#
		 * doHandle(org.eclipse.sensinact.gateway.core.message.SnaResponseMessage)
		 */
		@Override
		public void doHandle(SnaResponseMessage<?> message)
		{
			RemoteSensiNact.this.endpoint().dispatch(this.agentId, message);
		}

		/**
		 * @inheritDoc
		 *
		 * @see org.eclipse.sensinact.gateway.core.message.SnaAgentCallback#
		 * stop()
		 */
		@Override
		public void stop()
		{
			RemoteSensiNact.this.mediator.debug(
				"RemoteSensiNactCallback '%s' stopped", this.agentId);
		}
	}
	
	
	//********************************************************************//
	//						ABSTRACT DECLARATIONS						  //
	//********************************************************************//
	
	//********************************************************************//
	//						STATIC DECLARATIONS		      				  //
	//********************************************************************//

	public static final String NAMESPACE_PROP = "namespace";
	
	//********************************************************************//
	//						INSTANCE DECLARATIONS						  //
	//********************************************************************//
	
	protected Mediator mediator;
	protected LocalEndpoint localEndpoint;
	protected RemoteEndpoint remoteEndpoint;

	protected ServiceRegistration<RemoteCore> registration;
	protected List<String> agents;


	/**
	 * Constructor
	 * 
	 * @param mediator the {@link Mediator} allowing to interact with
	 * the OSGi host environment
	 * @param remoteEndpoint the {@link RemoteEndpoint} implementation
	 * the {@link RemoteCore} to be instantiated will be attached to in 
	 * manner of communicating with a remote instance of sensiNact
	 * @param localID the integer identifier of the {@link RemoteCore}
	 * to be instantiated, in the local instance of sensiNact
	 */
	protected RemoteSensiNact(
		Mediator mediator,
		AbstractRemoteEndpoint remoteEndpoint, 
		LocalEndpoint localEndpoint)
	{
		this.mediator = mediator;
		this.agents = new ArrayList<String>();
		
		this.remoteEndpoint = remoteEndpoint;
		if(this.remoteEndpoint == null)
		{
			throw new NullPointerException("No remote endpoint");
		}
		this.localEndpoint = localEndpoint;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#open(java.lang.String)
	 */
	@Override
	public void open(final String namespace)
	{
		AccessController.<Void>doPrivileged(new PrivilegedAction<Void>()
		{
			@Override
			public Void run()
			{		
				Dictionary<String,Object> props = new Hashtable<String,Object>();
		    	props.put(NAMESPACE_PROP, namespace);
		    	
				RemoteSensiNact.this.registration = mediator.getContext(
					).registerService(RemoteCore.class, RemoteSensiNact.this, 
						   props);
				
				mediator.debug("RemoteCore '%s' registration done", 
						namespace);
				return null;
			}
		});
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#close()
	 */
	@Override
	public void close()
	{
		AccessController.<Void>doPrivileged(new PrivilegedAction<Void>()
		{
			@Override
			public Void run()
			{
				if(RemoteSensiNact.this.registration != null)
				{
					try
					{
						RemoteSensiNact.this.registration.unregister();
						
					} catch(IllegalStateException e)
					{
						RemoteSensiNact.this.mediator.error(e);
					}
				}	
				if(!RemoteSensiNact.this.agents.isEmpty())
				{
					StringBuilder builder = new StringBuilder();
					builder.append("(&(org.eclipse.sensinact.gateway.agent.local=false)");
					builder.append("(|");
					while(!RemoteSensiNact.this.agents.isEmpty())
					{
						builder.append("(");
						builder.append("org.eclipse.sensinact.gateway.agent.id=");
						builder.append(RemoteSensiNact.this.agents.remove(0));
						builder.append(")");
					}
					builder.append(")");
					RemoteSensiNact.this.mediator.callServices(SnaAgent.class, 
					builder.toString(), new Executable<SnaAgent, Void>()
					{
						@Override
						public Void execute(SnaAgent snaAgent)
						        throws Exception
						{
							snaAgent.stop();
							return null;
						}
					});
				}
				return null;
			}
		});
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#endpoint()
	 */
	@Override
	public RemoteEndpoint endpoint()
	{
		return this.remoteEndpoint;
	}
	
  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getLocations(java.lang.String)
  	 */
	@Override
  	public JSONObject getLocations(String publicKey)
  	{
  		return this.localEndpoint.getLocations(publicKey);
  	}

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getAll(java.lang.String)
  	 */
	@Override
  	public JSONObject getAll(String publicKey)
  	{
  		return this.getAll(publicKey, null);
  	}
  	
  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getAll(java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getAll(String publicKey, String filter)
  	{
  		return this.localEndpoint.getAll(publicKey, filter);
  	}
  	
  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#getProviders(java.lang.String)
  	 */
	@Override
  	public JSONObject getProviders(String publicKey)
    {
  		return this.localEndpoint.getProviders(publicKey);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getProvider(java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getProvider(String publicKey, String serviceProviderId)
    {
  		return this.localEndpoint.getProvider(publicKey, serviceProviderId);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getServices(java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getServices(String publicKey, String serviceProviderId)
    {
  		return this.localEndpoint.getServices(publicKey, serviceProviderId);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getService(java.lang.String, java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getService(String publicKey, 
  			String serviceProviderId,String serviceId)
    {
  		return this.localEndpoint.getService(publicKey, 
  				serviceProviderId, serviceId);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getResources(java.lang.String, java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getResources(String publicKey, 
  			String serviceProviderId, String serviceId)
    {
  		return this.localEndpoint.getResources(publicKey, 
  				serviceProviderId, serviceId);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * getResource(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
  	 */
	@Override
  	public JSONObject getResource(String publicKey, 
  			String serviceProviderId, String serviceId, String resourceId)
    {
  		return this.localEndpoint.getResource(publicKey, serviceProviderId, 
  				serviceId, resourceId);
    }
  	
  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * get(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
  	 * java.lang.String)
  	 */
	@Override
  	public JSONObject get(String publicKey, String serviceProviderId, 
    		String serviceId, String resourceId, 
    		String attributeId)
    {
  		return this.localEndpoint.get(publicKey, serviceProviderId, serviceId,
  				resourceId, attributeId);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * set(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
  	 * java.lang.String, java.lang.Object)
  	 */
	@Override
  	public JSONObject set(String publicKey, String serviceProviderId,
           String serviceId, String resourceId, 
           String attributeId, Object parameter)
    {
  		return this.localEndpoint.set(publicKey, serviceProviderId, serviceId,
  				resourceId, attributeId, parameter);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * act(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
  	 * java.lang.Object[])
  	 */
	@Override
  	public JSONObject act(String publicKey, String serviceProviderId,
            String serviceId, String resourceId, 
            Object[] parameters )
    {
  		return this.localEndpoint.act(publicKey, serviceProviderId, serviceId,
  				resourceId, parameters);
    }

  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#
  	 * subscribe(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
  	 *  org.json.JSONArray)
  	 */
	@Override
  	public JSONObject subscribe(String publicKey, String serviceProviderId,
            String serviceId, String resourceId, JSONArray conditions)
    {  	
  		return this.subscribe(publicKey, serviceProviderId, serviceId,
  				resourceId, this.remoteEndpoint, conditions);  
    }

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
	 * subscribe(java.lang.String, java.lang.String, java.lang.String, 
	 * java.lang.String, org.eclipse.sensinact.gateway.core.message.Recipient, 
	 * org.json.JSONArray)
	 */
	@Override
	public JSONObject subscribe(String publicKey, String serviceProviderId,
	        String serviceId, String resourceId, Recipient recipient,
	        JSONArray conditions)
	{
  		return this.localEndpoint.subscribe(publicKey, serviceProviderId, serviceId,
  				resourceId, recipient, conditions);  
	}
	
  	/**
  	 * @inheritDoc
  	 *
  	 * @see org.eclipse.sensinact.gateway.core.Endpoint#
  	 * unsubscribe(java.lang.String, java.lang.String, java.lang.String, java.lang.String, 
  	 * java.lang.String)
  	 */
	@Override
  	public JSONObject unsubscribe(String publicKey, String serviceProviderId,
            String serviceId, String resourceId, 
           String subscriptionId )
    {
  		return this.localEndpoint.unsubscribe(publicKey, 
  			serviceProviderId, serviceId, resourceId, subscriptionId);   
    }

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#namespace()
	 */
	@Override
	public String namespace() 
	{
		ServiceReference<Core> ref = mediator.getContext(
				).getServiceReference(Core.class);
		
		if(ref!=null )
		{
			Core core = mediator.getContext().getService(ref);
			return core.namespace();
		}
		return null;
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#localID()
	 */
	@Override
	public int localID() 
	{
		return this.localEndpoint.localID();
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#
	 * registerAgent(java.lang.String, org.eclipse.sensinact.gateway.core.message.SnaFilter,
	 *  java.lang.String)
	 */
	@Override
	public void registerAgent(final String agentId, 
			final SnaFilter filter, final String agentKey )
	{			
		AccessController.<Void>doPrivileged(new PrivilegedAction<Void>()
		{
			@Override
			public Void run()
			{				
				final SnaAgentImpl agent = SnaAgentImpl.createAgent(
					mediator, new RemoteSensiNactCallback(agentId), 
						filter, agentKey);				
				Dictionary<String,Object> props = new Hashtable<String,Object>();
				props.put("org.eclipse.sensinact.gateway.agent.id", agentId);
			    props.put("org.eclipse.sensinact.gateway.agent.local", false);
				agent.start(props);
				return null;
			}
		});
	}
	
	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#unregisterAgent(java.lang.String)
	 */
	@Override
	public void unregisterAgent(String agentId)
	{
		this.callAgent(agentId, false, new Executable<SnaAgent,Void>()
		{
			@Override
			public Void execute(SnaAgent agent) throws Exception
			{
				if(agent != null)
				{
					agent.stop();
				}
				return null;
			}	
		});
	}

	/**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.RemoteCore#
	 * dispatch(java.lang.String, org.eclipse.sensinact.gateway.core.message.SnaMessage)
	 */
	@Override
	public void dispatch(String agentId, final SnaMessage<?> message)
	{
		this.callAgent(agentId, true, new Executable<SnaAgent,Void>()
		{
			@Override
			public Void execute(SnaAgent agent) throws Exception
			{
				if(agent != null)
				{
					agent.register(message);
				}
				return null;
			}	
		});
	} 

	/**
	 * @param agentId
	 * @param local
	 * @param executable
	 */
	private void callAgent(final String agentId, final boolean local,
			final Executable<SnaAgent,Void> executable)
	{
		AccessController.<Void>doPrivileged(new PrivilegedAction<Void>()
		{
			@Override
			public Void run()
			{
				return RemoteSensiNact.this.mediator.callService(SnaAgent.class, 
				new StringBuilder().append("(&(org.eclipse.sensinact.gateway.agent.id="
				).append(agentId).append(")(org.eclipse.sensinact.gateway.agent.local="
					).append(Boolean.toString(local)).append("))"
							).toString(),executable);
			}
		});
	}
}
