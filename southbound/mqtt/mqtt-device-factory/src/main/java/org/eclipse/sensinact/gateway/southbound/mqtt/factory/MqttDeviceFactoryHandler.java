/*********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
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
package org.eclipse.sensinact.gateway.southbound.mqtt.factory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.sensinact.gateway.southbound.device.factory.DeviceFactoryException;
import org.eclipse.sensinact.gateway.southbound.device.factory.IDeviceMappingHandler;
import org.eclipse.sensinact.gateway.southbound.device.factory.dto.DeviceMappingConfigurationDTO;
import org.eclipse.sensinact.gateway.southbound.mqtt.api.IMqttMessage;
import org.eclipse.sensinact.gateway.southbound.mqtt.api.IMqttMessageListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * MQTT device factory handler
 */
@Component(service = {}, configurationPid = "sensinact.mqtt.device.factory", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class MqttDeviceFactoryHandler implements IMqttMessageListener {

    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(MqttDeviceFactoryHandler.class);

    /**
     * Device mapping service
     */
    @Reference
    private IDeviceMappingHandler mappingHandler;

    /**
     * Device mapping service configuration
     */
    private DeviceMappingConfigurationDTO mappingConfiguration;

    /**
     * ID of the MQTT handler that we can accept messages from, if defined
     */
    private String allowedHandlerId;

    /**
     * Listener service registration
     */
    private ServiceRegistration<IMqttMessageListener> svcReg;

    /**
     * Component activated
     */
    @Activate
    void activate(final ComponentContext componentContext, final MqttDeviceFactoryConfiguration configuration) {

        // Check MQTT configuration
        final String handlerId = configuration.mqtt_handler_id();
        String[] topics = configuration.mqtt_topics();
        if ((handlerId == null || handlerId.isBlank()) && (topics == null || topics.length == 0)) {
            throw new IllegalArgumentException(
                    "MQTT device factory requires at least a handler ID or a topic to be configured");
        }

        allowedHandlerId = handlerId;

        if (topics == null || topics.length == 0) {
            topics = new String[] { "#" };
        } else if (topics.length == 1 && topics[0].contains(",")) {
            topics = topics[0].split(",");
        }

        // Check mapping configuration
        if (configuration.mapping() == null) {
            throw new IllegalArgumentException("No mapping configuration given");
        }

        // Convert the mapping configuration
        try {
            mappingConfiguration = new ObjectMapper().readValue(configuration.mapping(),
                    DeviceMappingConfigurationDTO.class);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing MQTT mapping configuration", e);
            throw new IllegalArgumentException("Invalid mapping configuration", e);
        }

        // Register the listener service
        final BundleContext context = componentContext.getBundleContext();
        final Hashtable<String, Object> properties = new Hashtable<>();
        if (topics != null) {
            properties.put(MQTT_TOPICS_FILTERS, topics);
        }
        svcReg = context.registerService(IMqttMessageListener.class, this, properties);
    }

    /**
     * Component deactivated
     */
    @Deactivate
    void deactivate() {
        if (svcReg != null) {
            svcReg.unregister();
            svcReg = null;
        }
    }

    @Override
    public void onMqttMessage(String handlerId, String topic, IMqttMessage message) {
        if (allowedHandlerId != null && !allowedHandlerId.equals(handlerId)) {
            // Ignore message
            return;
        }

        Map<String, String> context = new HashMap<>();
        context.put("handlerId", handlerId);
        fillTopicSegments(topic, context);

        try {
            mappingHandler.handle(mappingConfiguration, context, message.getPayload());
        } catch (DeviceFactoryException e) {
            logger.error("Error handling MQTT payload from handler '{}' on topic '{}': {}", handlerId, topic,
                    e.getMessage(), e);
        }
    }

    private void fillTopicSegments(String topic, Map<String, String> context) {
        context.put("topic", topic);

        int i = 0;
        String segment;
        int idx = 0;

        do {
            int slash = topic.indexOf('/', idx);
            if (slash > 0) {
                segment = topic.substring(idx, slash);
                idx = slash + 1;
            } else {
                segment = topic.substring(idx);
                idx = -1;
            }
            context.put("topic-".concat(Integer.toString(i)), segment);
            i++;
        } while (idx >= 0);

        context.put("topic-last", segment);
    }
}
