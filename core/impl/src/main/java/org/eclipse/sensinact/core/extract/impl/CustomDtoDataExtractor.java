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
package org.eclipse.sensinact.core.extract.impl;

import java.util.List;
import java.util.function.Function;

import org.eclipse.sensinact.core.dto.impl.AbstractUpdateDto;

public class CustomDtoDataExtractor implements DataExtractor {

    private final Function<Object, List<? extends AbstractUpdateDto>> mapper;

    public CustomDtoDataExtractor(Class<?> clazz) {
        mapper = AnnotationMapping.getUpdateDtoMappings(clazz);
    }

    @Override
    public List<? extends AbstractUpdateDto> getUpdates(Object update) {
        return mapper.apply(update);
    }
}
