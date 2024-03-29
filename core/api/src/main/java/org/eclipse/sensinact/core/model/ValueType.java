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
package org.eclipse.sensinact.core.model;

/**
 * The type of the value
 */
public enum ValueType {

    /**
     * The value can be modified by a SET operation
     */
    MODIFIABLE,
    /**
     * This value may change over time, but not SET
     */
    UPDATABLE,
    /**
     * This value cannot be SET, and will not change over time
     */
    FIXED;
}
