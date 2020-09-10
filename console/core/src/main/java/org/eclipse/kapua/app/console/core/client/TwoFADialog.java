/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.app.console.core.client;

import org.eclipse.kapua.app.console.module.api.client.messages.ConsoleMessages;
import org.eclipse.kapua.app.console.module.api.client.resources.icons.KapuaIcon;
import org.eclipse.kapua.app.console.module.api.client.ui.dialog.SimpleDialog;
import org.eclipse.kapua.app.console.module.api.client.util.DialogUtils;
import org.eclipse.kapua.app.console.module.api.shared.model.session.GwtSession;
import org.eclipse.kapua.app.console.module.authentication.client.messages.ConsoleCredentialMessages;
import org.eclipse.kapua.app.console.module.authentication.client.tabs.credentials.TwoFAManagementPanel;

import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.google.gwt.core.client.GWT;

public class TwoFADialog extends SimpleDialog {

    private static final ConsoleCredentialMessages CREDENTIAL_MSGS = GWT.create(ConsoleCredentialMessages.class);
    private static final ConsoleMessages MSGS = GWT.create(ConsoleMessages.class);

    private GwtSession currentSession;

    private TwoFAManagementPanel twoFAManagementPanel;

    public TwoFADialog(GwtSession currentSession, String userIdStr, String username) {
        this.currentSession = currentSession;
        this.twoFAManagementPanel = new TwoFAManagementPanel(currentSession, userIdStr, username, this);
        DialogUtils.resizeDialog(this, 600, 530);
    }

    @Override
    public void createBody() {
        bodyPanel.setLayout(new FitLayout());
        bodyPanel.add(twoFAManagementPanel);
    }

    @Override
    protected void addListeners() {
    }

    @Override
    public void submit() {

    }

    @Override
    public String getHeaderMessage() {
        return CREDENTIAL_MSGS.twoFADialogHeader();
    }

    @Override
    public KapuaIcon getInfoIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInfoMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createButtons() {
        getButtonBar().removeAll();

        cancelButton = new Button(getCancelButtonText());
        cancelButton.setSize(60, 25);
        cancelButton.setStyleAttribute("margin-left", "3px");
        cancelButton.addSelectionListener(new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                exitStatus = null;
                hide();
            }
        });

        addButton(cancelButton);
    }

    @Override
    protected String getCancelButtonText() {
        return MSGS.closeButton();
    }

}
