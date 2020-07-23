package org.eclipse.sensinact.gateway.core;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.common.execution.Executable;
import org.eclipse.sensinact.gateway.core.method.AccessMethod;
import org.eclipse.sensinact.gateway.core.remote.SensinactCoreBaseIFaceManager;
import org.eclipse.sensinact.gateway.core.security.AccessLevelOption;
import org.eclipse.sensinact.gateway.core.security.AccessNode;
import org.eclipse.sensinact.gateway.core.security.AccessTree;
import org.eclipse.sensinact.gateway.util.UriUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

@SuppressWarnings({"rawtypes","unchecked"}) 
public final class RegistryEndpoint {
	
	private Mediator mediator;
	private final String defaultLocation;
	
    public RegistryEndpoint(Mediator mediator){
        this.mediator = mediator;
		this.defaultLocation = ModelInstance.defaultLocation(mediator);
    }

	protected Collection<ServiceReference<SensiNactResourceModel>> getReferences(SessionKey sessionKey, String filter) {
        return getReferences(sessionKey.getAccessTree(),filter);
    }

    protected Collection<ServiceReference<SensiNactResourceModel>> getReferences(AccessTree<? extends AccessNode> tree, String filter) {
        AccessMethod.Type describe = AccessMethod.Type.valueOf(AccessMethod.DESCRIBE);
        Collection<ServiceReference<SensiNactResourceModel>> result = new ArrayList<ServiceReference<SensiNactResourceModel>>();
        Collection<ServiceReference<SensiNactResourceModel>> references = null;
        try {
            references = mediator.getContext().getServiceReferences(SensiNactResourceModel.class, filter);
            Iterator<ServiceReference<SensiNactResourceModel>> iterator = references.iterator();

            while (iterator.hasNext()) {
                ServiceReference<SensiNactResourceModel> reference = iterator.next();
                String name = (String) reference.getProperty("name");
                Integer level = (Integer) reference.getProperty(name.concat(".DESCRIBE"));
                if (level == null) {
                    level = Integer.valueOf(AccessLevelOption.OWNER.getAccessLevel().getLevel());
                }
                AccessNode node = tree.getRoot().get(UriUtils.getUri(new String[] { name }));
                if (node == null) {
                    node = tree.getRoot();
                }
                if (node.getAccessLevelOption(describe).getAccessLevel().getLevel() >= level.intValue()) {
                    result.add(reference);
                }
            }
        } catch (InvalidSyntaxException e) {
           mediator.error(e.getMessage(), e);
        }
        return result;
    }

    public Set<ServiceProvider> serviceProviders(final SessionKey sessionKey, String filter) {
        String activeFilter = "(lifecycle.status=ACTIVE)";
        String providersFilter = null;

        if (filter == null) {
            providersFilter = activeFilter;

        } else {
            StringBuilder filterBuilder = new StringBuilder().append("(&");
            if (!filter.startsWith("(")) {
                filterBuilder.append("(");
            }
            filterBuilder.append(filter);
            if (!filter.endsWith(")")) {
                filterBuilder.append(")");
            }
            filterBuilder.append(activeFilter);
            filterBuilder.append(")");
            providersFilter = filterBuilder.toString();
        }
        final String fltr = providersFilter;

        Set<ServiceProvider> serviceProviders = AccessController.<Set<ServiceProvider>>doPrivileged(
                new PrivilegedAction<Set<ServiceProvider>>() {
                    @Override
                    public Set<ServiceProvider> run() {
                        Collection<ServiceReference<SensiNactResourceModel>> references =
                                RegistryEndpoint.this.getReferences(sessionKey, fltr);

                        Iterator<ServiceReference<SensiNactResourceModel>> iterator =
                                references.iterator();

                        Set<ServiceProvider> providers = new HashSet<ServiceProvider>();

                        while (iterator.hasNext()) {
                            ServiceReference<SensiNactResourceModel> ref = iterator.next();
                            SensiNactResourceModel model = mediator.getContext().getService(ref);
                            ServiceProvider provider = null;
                            try {
                                provider = (ServiceProvider) model.getRootElement()
                                        .getProxy(sessionKey.getAccessTree());

                            } catch (ModelElementProxyBuildException e) {
                                mediator.error(e);
                            }
                            if (provider != null && provider.isAccessible()) {
                                providers.add(provider);
                            }
                        }
                        return providers;
                    }
                });
        return serviceProviders;
    }

    public ServiceProvider serviceProvider(SessionKey sessionKey, final String serviceProviderName) {
        ServiceProvider provider = null;

        Set<ServiceProvider> providers = this.serviceProviders(sessionKey,
                new StringBuilder().append("(name=").append(serviceProviderName).append(")").toString());

        if (providers == null || providers.size() != 1) {
            return provider;
        }
        provider = providers.iterator().next();
        return provider;
    }

    public Service service(SessionKey sessionKey, String serviceProviderName, String serviceName) {
        ServiceProvider serviceProvider = serviceProvider(sessionKey, serviceProviderName);
        Service service = null;
        if (serviceProvider != null) {
            service = serviceProvider.getService(serviceName);
        }
        return service;
    }

    public Resource resource(SessionKey sessionKey, String serviceProviderName, String serviceName,
                              String resourceName) {
        Service service = this.service(sessionKey, serviceProviderName, serviceName);
        Resource resource = null;
        if (service != null) {
            resource = service.getResource(resourceName);
        }
        return resource;
    }

    public boolean isAccessible(AccessTree<? extends AccessNode> tree, String path) {
        String[] uriElements = UriUtils.getUriElements(path);
        String providerName = uriElements[0];
        String serviceName = uriElements.length>1?uriElements[1]:null;
        String resourceName = uriElements.length>2?uriElements[2]:null;

        String filter = new StringBuilder().append("(&(name=").append(providerName
        ).append(")(lifecycle.status=ACTIVE))").toString();

        Collection<ServiceReference<SensiNactResourceModel>> references =
                this.getReferences(tree, filter);
        if(references.size()!=1) {
            return false;
        }
        ServiceReference<SensiNactResourceModel> reference = references.iterator().next();

        AccessMethod.Type describe = AccessMethod.Type.valueOf(AccessMethod.DESCRIBE);
        AccessNode node = tree.getRoot();

        int index = 0;
        String pa = null;
        String key = null;

        Integer inheritedObjectDescribeLevel = Integer.valueOf(
                AccessLevelOption.OWNER.getAccessLevel().getLevel());

        while(true) {
            switch(index) {
                case 0:
                    pa = UriUtils.getUri(new String[] {providerName});
                    key = providerName.concat(".DESCRIBE");
                    break;
                case 1:
                    List<String> services = (List<String>) reference.getProperty("services");
                    if(services == null || !services.contains(serviceName)) {
                        return false;
                    }
                    pa = UriUtils.getUri(new String[] {pa,serviceName});
                    key =  new StringBuilder().append(serviceName).append(".DESCRIBE").toString();
                    break;
                case 2:
                    List<String> resources = (List<String>) reference.getProperty(serviceName.concat(".resources"));
                    if(resources == null || !resources.contains(resourceName)) {
                        return false;
                    }
                    pa = UriUtils.getUri(new String[] {pa,resourceName});
                    key =  new StringBuilder().append(serviceName).append(".").append(resourceName).append(".DESCRIBE").toString();
                    break;
                default:
                    return false;
            }
            AccessNode tmpNode = tree.getRoot().get(pa);
            if (tmpNode == null) {
                tmpNode = node;
            } else{
                node = tmpNode;
            }
            Integer level =  (Integer) reference.getProperty(key);
            if(level == null) {
                level = inheritedObjectDescribeLevel;
            }else {
                inheritedObjectDescribeLevel = level;
            }
            if (node.getAccessLevelOption(describe).getAccessLevel().getLevel() < inheritedObjectDescribeLevel.intValue()) {
                return false;
            }
            if(++index == uriElements.length) {
                return true;
            }
        }
    }

    public String getAll(SessionKey sessionKey, boolean resolveNamespace, String filter) {
        StringBuilder builder = new StringBuilder();
        int index = -1;

        Collection<ServiceReference<SensiNactResourceModel>> references = 
        		RegistryEndpoint.this.getReferences(sessionKey, filter);
        Iterator<ServiceReference<SensiNactResourceModel>> iterator = 
        		references.iterator();

        AccessTree<? extends AccessNode> tree = sessionKey.getAccessTree();
        AccessMethod.Type describe = AccessMethod.Type.valueOf(AccessMethod.DESCRIBE);
        
        String namespace = namespace();
        String prefix = "";
        
        while (iterator.hasNext()) {
            index++;
            ServiceReference<SensiNactResourceModel> reference = iterator.next();
            String name = (String) reference.getProperty("name");

            String provider = new StringBuilder().append(prefix).append(name).toString();
            
            String location = null;
            Object obj = reference.getProperty(ModelInstanceRegistration.LOCATION_PROPERTY.concat(".value"));
            if (obj != null) {
                location = String.valueOf(obj).replace('"', '\'');
            }
            location = (location == null || location.length() == 0) ? defaultLocation : location;
            List<String> serviceList = (List<String>) reference.getProperty("services");

            builder.append(index > 0 ? ',' : "");
            builder.append('{');
            builder.append("\"name\":");
            builder.append('"');
            if(!SensinactCoreBaseIFaceManager.EMPTY_NAMESPACE.equals(namespace) && sessionKey.localID()!=0) { 
            	builder.append(namespace);
            	builder.append(":");
            }
            builder.append(provider);
            builder.append('"');
            if(!SensinactCoreBaseIFaceManager.EMPTY_NAMESPACE.equals(namespace)) {
	            builder.append(",\"namespace\":");
	            builder.append('"');
	            builder.append(namespace);
	            builder.append('"');
            }
            builder.append(",\"location\":");
            builder.append('"');
            builder.append(location);
            builder.append('"');
            builder.append(",\"services\":");
            builder.append('[');

            int sindex = 0;
            int slength = serviceList == null ? 0 : serviceList.size();
            for (; sindex < slength; sindex++) {
                String service = serviceList.get(sindex);
                String serviceUri = UriUtils.getUri(new String[] { name, service });
                Integer serviceLevel = (Integer) reference.getProperty(service.concat(".DESCRIBE"));
                if (serviceLevel == null) {
                    serviceLevel = Integer.valueOf(AccessLevelOption.OWNER.getAccessLevel().getLevel());
                }
                AccessNode node = sessionKey.getAccessTree().getRoot().get(serviceUri);
                if (node == null) {
                    node = tree.getRoot();
                }
                int describeAccessLevel = node.getAccessLevelOption(describe).getAccessLevel().getLevel();
                int serviceLevelLevel = serviceLevel.intValue();

                if (node.getAccessLevelOption(describe).getAccessLevel().getLevel() < serviceLevel.intValue()) {
                    continue;
                }
                List<String> resourceList = (List<String>) reference.getProperty(service.concat(".resources"));

                builder.append(sindex > 0 ? ',' : "");
                builder.append('{');
                builder.append("\"name\":");
                builder.append('"');
                builder.append(service);
                builder.append('"');
                builder.append(",\"resources\":");
                builder.append('[');

                int rindex = 0;
                int rlength = resourceList == null ? 0 : resourceList.size();
                for (; rindex < rlength; rindex++) {
                    String resource = resourceList.get(rindex);
                    String resolvedResource = new StringBuilder().append(service).append(".").append(resource)
                            .toString();
                    String resourceUri = UriUtils.getUri(new String[] { name, service, resource });
                    Integer resourceLevel = (Integer) reference.getProperty(resolvedResource.concat(".DESCRIBE"));
                    if (resourceLevel == null) {
                        resourceLevel = Integer.valueOf(AccessLevelOption.OWNER.getAccessLevel().getLevel());
                    }
                    node = sessionKey.getAccessTree().getRoot().get(resourceUri);
                    if (node == null) {
                        node = tree.getRoot();
                    }
                    if (node.getAccessLevelOption(describe).getAccessLevel().getLevel() < resourceLevel
                            .intValue()) {
                        continue;
                    }
                    String type = (String) reference.getProperty(resolvedResource.concat(".type"));
                    builder.append(rindex > 0 ? ',' : "");
                    builder.append('{');
                    builder.append("\"name\":");
                    builder.append('"');
                    builder.append(resource);
                    builder.append('"');
                    builder.append(",\"type\":");
                    builder.append('"');
                    builder.append(type);
                    builder.append('"');
                    builder.append('}');
                }
                builder.append(']');
                builder.append('}');
            }
            builder.append(']');
            builder.append('}');
        }
        String content = builder.toString();
        return content;
    }

    public String getProviders(SessionKey sessionKey, boolean resolveNamespace, String filter) {
        try{
            String prefix = resolveNamespace
                    ? new StringBuilder().append(namespace()).append(":").toString()
                    : "";
            Collection<ServiceReference<SensiNactResourceModel>> references = this.getReferences(sessionKey, filter);
            Iterator<ServiceReference<SensiNactResourceModel>> iterator = references.iterator();

            StringBuilder builder = new StringBuilder();
            int index = 0;
            while (iterator.hasNext()) {
                ServiceReference<SensiNactResourceModel> reference = iterator.next();
                String name = (String) reference.getProperty("name");
                String provider = new StringBuilder().append(prefix).append(name).toString();
                if (index > 0) {
                    builder.append(",");
                }
                builder.append('"');
                builder.append(provider);
                builder.append('"');
                index++;
            }
            String content = builder.toString();
            return content;
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }
    
    /**
	 * @inheritDoc
	 *
	 * @see org.eclipse.sensinact.gateway.core.Core#namespace()
	 */
	protected String namespace() {
		String namespace = this.mediator.callService(SensinactCoreBaseIFaceManager.class, 
			new Executable<SensinactCoreBaseIFaceManager,String>() {
				@Override
				public String execute(SensinactCoreBaseIFaceManager sensinactCoreBaseIFaceManager) throws Exception {							
					return sensinactCoreBaseIFaceManager.namespace();
				}
			}
		);
		return namespace==null?SensinactCoreBaseIFaceManager.EMPTY_NAMESPACE:namespace;
	}
};