package org.eclipse.sensinact.prototype.action;

import java.time.Instant;
import java.util.List;

import org.eclipse.sensinact.prototype.annotation.propertytype.ProviderName;
import org.eclipse.sensinact.prototype.annotation.propertytype.WhiteboardResource;
import org.eclipse.sensinact.prototype.annotation.verb.ACT;
import org.osgi.service.component.annotations.Component;

/**
 * Service properties define the provider that this resource is for
 */
@WhiteboardResource
@ProviderName("pull_based")
@Component(service = _04_ParameterActionResource.class)
public class _04_ParameterActionResource {
	
	/**
	 * A GET method for a service and resource
	 * @return
	 */
	@ACT(service = "example", resource = "default")
	public List<Long> doAction(Instant fromTime, Instant toTime) {
		// Run the action and return the result
		return null;
	}
}
