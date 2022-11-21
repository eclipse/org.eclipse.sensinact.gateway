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
package org.eclipse.sensinact.northbound.rest.dto;

import java.util.List;

/**
 * Lists the IDs of known providers
 */
public class ResultProvidersListDTO extends ResultRootDTO {

    /**
     * List of provider IDs
     */
    public List<String> providers;
}