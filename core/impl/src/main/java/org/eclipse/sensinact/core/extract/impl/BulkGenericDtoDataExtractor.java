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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.sensinact.core.push.dto.BulkGenericDto;
import org.eclipse.sensinact.core.push.dto.GenericDto;
import org.eclipse.sensinact.core.dto.impl.AbstractUpdateDto;

public class BulkGenericDtoDataExtractor implements DataExtractor {

    private final GenericDtoDataExtractor subMapper = new GenericDtoDataExtractor();

    @Override
    public List<? extends AbstractUpdateDto> getUpdates(Object update) {

        BulkGenericDto dto = checkCast(update);

        List<GenericDto> list = dto.dtos == null ? Collections.emptyList() : dto.dtos;

        return list.stream().map(subMapper::getUpdates).flatMap(List::stream).collect(Collectors.toList());
    }

    private BulkGenericDto checkCast(Object update) {
        try {
            return BulkGenericDto.class.cast(update);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("The supplied update dto is not of the correct type to extract", e);
        }
    }
}
