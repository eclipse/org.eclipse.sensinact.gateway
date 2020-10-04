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
package org.eclipse.sensinact.gateway.common.constraint;

import org.eclipse.sensinact.gateway.util.CastUtils;
import org.eclipse.sensinact.gateway.util.JSONUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Constraint on the difference in percents between two values
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
public class Delta implements Constraint {
    protected static final Logger LOGGER = Logger.getLogger(Delta.class.getCanonicalName());

    public static final String OPERATOR = "diff";
    private final double delta;

    private final double referenceValue;

    private final boolean complement;
    /**
     * Mediator used to interact with the OSGi host
     * environment
     */
    protected final ClassLoader classloader;

    /**
     * @param delta
     * @param referenceValue
     * @throws InvalidConstraintDefinitionException
     */
    public Delta(ClassLoader classloader, Object delta, Object referenceValue, boolean complement) throws InvalidConstraintDefinitionException {
        this.classloader = classloader;
        this.complement = complement;
        try {
            double deltaValue = Math.abs(CastUtils.cast(this.classloader, double.class, delta));
            this.delta = deltaValue > 100 ? deltaValue - 100 : deltaValue;

            this.referenceValue = CastUtils.cast(this.classloader, double.class, referenceValue);
        } catch (ClassCastException e) {
            throw new InvalidConstraintDefinitionException("Unable to cast delta and referenceValue arguments into double values");
        } catch (NullPointerException e) {
            throw new InvalidConstraintDefinitionException("delta and referenceValue arguments must be not null");
        }
    }

    /**
     * @inheritDoc
     * @see Constraint#complies(java.lang.Object)
     */
    @Override
    public boolean complies(Object value) {
        boolean complies = false;
        double doubleValue = 0;
        try {
            doubleValue = CastUtils.cast(classloader, double.class, value);
        } catch (Exception e) {
            return complies;
        }
        complies = Math.abs(100 - Math.abs((100d * doubleValue) / this.referenceValue)) >= delta;
        return complies ^ isComplement();
    }

    /**
     * @inheritDoc
     * @see Constraint#getOperator()
     */
    @Override
    public String getOperator() {
        return OPERATOR;
    }

    /**
     * @inheritDoc
     * @see Constraint#isComplement()
     */
    @Override
    public boolean isComplement() {
        return this.complement;
    }

    /**
     * @inheritDoc
     * @see JSONable#getJSON()
     */
    @Override
    public String getJSON() {
        StringBuilder builder = new StringBuilder();
        builder.append(JSONUtils.OPEN_BRACE);
        builder.append(JSONUtils.QUOTE);
        builder.append(OPERATOR_KEY);
        builder.append(JSONUtils.QUOTE);
        builder.append(JSONUtils.COLON);
        builder.append(JSONUtils.QUOTE);
        builder.append(this.getOperator());
        builder.append(JSONUtils.QUOTE);
        builder.append(JSONUtils.COMMA);
        builder.append(JSONUtils.QUOTE);
        builder.append(COMPLEMENT_KEY);
        builder.append(JSONUtils.QUOTE);
        builder.append(JSONUtils.COLON);
        builder.append(this.complement);
        builder.append(JSONUtils.COMMA);
        builder.append(JSONUtils.QUOTE);
        builder.append(OPERAND_KEY);
        builder.append(JSONUtils.QUOTE);
        builder.append(JSONUtils.COLON);
        builder.append(JSONUtils.OPEN_BRACKET);
        builder.append(this.delta);
        builder.append(JSONUtils.COMMA);
        builder.append(this.referenceValue);
        builder.append(JSONUtils.CLOSE_BRACKET);
        builder.append(JSONUtils.CLOSE_BRACE);
        return builder.toString();
    }

    /**
     * @inheritDoc
     * @see Constraint#getComplement()
     */
    @Override
    public Constraint getComplement() {
        Delta complement = null;
        try {
            complement = new Delta(this.classloader, this.delta, this.referenceValue, !this.complement);
        } catch (InvalidConstraintDefinitionException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return complement;
    }
}
