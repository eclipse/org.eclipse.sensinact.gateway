/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
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
package org.eclipse.sensinact.core.command;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.sensinact.core.model.SensinactModelManager;
import org.eclipse.sensinact.core.twin.SensinactDigitalTwin;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

public class IndependentCommands<T> extends AbstractSensinactCommand<List<T>> {

    private List<AbstractSensinactCommand<? extends T>> commands;

    private SensinactDigitalTwin twin;

    private SensinactModelManager modelMgr;

    private PromiseFactory pf;

    public IndependentCommands(AbstractSensinactCommand<? extends T> command,
            AbstractSensinactCommand<? extends T> command2) {
        commands = List.of(command, command2);
    }

    public IndependentCommands(AbstractSensinactCommand<? extends T> command,
            AbstractSensinactCommand<? extends T> command2, AbstractSensinactCommand<? extends T> command3) {
        commands = List.of(command, command2, command3);
    }

    public IndependentCommands(List<AbstractSensinactCommand<? extends T>> command) {
        commands = List.copyOf(command);
    }

    @Override
    protected Promise<List<T>> call(SensinactDigitalTwin twin, SensinactModelManager modelMgr,
            PromiseFactory promiseFactory) {
        this.twin = twin;
        this.modelMgr = modelMgr;
        this.pf = promiseFactory;

        return promiseFactory.all(commands.stream().map(this::safeCall).collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    private Promise<T> safeCall(AbstractSensinactCommand<? extends T> command) {
        return (Promise<T>) safeCall(command, twin, modelMgr, pf);
    }
}
