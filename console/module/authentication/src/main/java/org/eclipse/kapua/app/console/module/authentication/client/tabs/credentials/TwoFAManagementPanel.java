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
package org.eclipse.kapua.app.console.module.authentication.client.tabs.credentials;

import java.util.Date;
import java.util.List;

import org.eclipse.kapua.app.console.module.api.client.resources.icons.IconSet;
import org.eclipse.kapua.app.console.module.api.client.resources.icons.KapuaIcon;
import org.eclipse.kapua.app.console.module.api.client.ui.button.KapuaButton;
import org.eclipse.kapua.app.console.module.api.client.ui.panel.ContentPanel;
import org.eclipse.kapua.app.console.module.api.client.util.CookieUtils;
import org.eclipse.kapua.app.console.module.api.client.util.FailureHandler;
import org.eclipse.kapua.app.console.module.api.shared.model.GwtXSRFToken;
import org.eclipse.kapua.app.console.module.api.shared.model.session.GwtSession;
import org.eclipse.kapua.app.console.module.api.shared.service.GwtSecurityTokenService;
import org.eclipse.kapua.app.console.module.api.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kapua.app.console.module.authentication.client.messages.ConsoleCredentialMessages;
import org.eclipse.kapua.app.console.module.authentication.shared.model.GwtMfaCredentialOptions;
import org.eclipse.kapua.app.console.module.authentication.shared.model.GwtMfaCredentialOptionsCreator;
import org.eclipse.kapua.app.console.module.authentication.shared.model.GwtScratchCode;
import org.eclipse.kapua.app.console.module.authentication.shared.model.permission.CredentialSessionPermission;
import org.eclipse.kapua.app.console.module.authentication.shared.service.GwtMfaCredentialOptionsService;
import org.eclipse.kapua.app.console.module.authentication.shared.service.GwtMfaCredentialOptionsServiceAsync;
import org.eclipse.kapua.app.console.module.authentication.shared.service.GwtScratchCodeService;
import org.eclipse.kapua.app.console.module.authentication.shared.service.GwtScratchCodeServiceAsync;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.WindowEvent;
import com.extjs.gxt.ui.client.widget.Dialog;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.Text;
import com.extjs.gxt.ui.client.widget.form.FieldSet;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.extjs.gxt.ui.client.widget.layout.FormLayout;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;

public class TwoFAManagementPanel extends ContentPanel {

    private static final ConsoleCredentialMessages MSGS = GWT.create(ConsoleCredentialMessages.class);
    private static final GwtSecurityTokenServiceAsync GWT_XSRF_SERVICE = GWT.create(GwtSecurityTokenService.class);
    private static final GwtMfaCredentialOptionsServiceAsync GWT_MFA_CREDENTIAL_OPTIONS_SERVICE = GWT.create(GwtMfaCredentialOptionsService.class);
    private static final GwtScratchCodeServiceAsync GWT_SCRATCH_CODES_SERVICE = GWT.create(GwtScratchCodeService.class);

    private final GwtSession currentSession;
    private final String selectedUserId;
    private final String selectedUserName;
    private final boolean selfManagement;
    private final Dialog selfManagementDialog;

    private GwtMfaCredentialOptions gwtMfaCredentialOptions;

    private Text scratchCodesArea;

    // 2FA Toolbar buttons
    private KapuaButton enable2FA;
    private KapuaButton forgetTrustedMachine;
    private KapuaButton help;
    private ContentPanel twoFAContentPanel;

    private boolean keyEnabled;

    // HELP
    private com.extjs.gxt.ui.client.widget.form.FormPanel helpPanel;
    private boolean toggleHelp;
    private FormData formData;

    private Image barcodeImage;
    private Image androidImage;
    private Image appleImage;
    private Image blankImage;

    private Text enabledText;

    public TwoFAManagementPanel(GwtSession currentSession, String selectedUserId, String selectedUserName) {
        this(currentSession, selectedUserId, selectedUserName, null);
    }

    public TwoFAManagementPanel(GwtSession currentSession, String selectedUserId, String selectedUserName, Dialog selfManagementDialog) {
        setHeaderVisible(false);
        this.currentSession = currentSession;
        this.selectedUserId = selectedUserId;
        this.selectedUserName = selectedUserName;
        this.selfManagement = selfManagementDialog != null;
        this.selfManagementDialog = selfManagementDialog;
    }

    // Build the 2FA toolbar
    protected ToolBar createToolBar() {
        ToolBar menuToolBar = new ToolBar();

        enable2FA = new KapuaButton(MSGS.mfaFormEnable2FA(), new KapuaIcon(IconSet.CHECK), new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent myce) {
                GWT_XSRF_SERVICE.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                        doUnmask();
                    }

                    @Override
                    public void onSuccess(final GwtXSRFToken xsrfToken) {
                        getButtonBar().disable();

                        keyEnabled = gwtMfaCredentialOptions != null && gwtMfaCredentialOptions.getAuthenticationKey() != null;
                        if (!keyEnabled) {
                            doMask(MSGS.maskEnableTwoFA());
                            // MFA is disabled, so enable it
                            final GwtMfaCredentialOptionsCreator gwtMfaCredentialOptionsCreator = new GwtMfaCredentialOptionsCreator();
                            gwtMfaCredentialOptionsCreator.setScopeId(currentSession.getSelectedAccountId());
                            gwtMfaCredentialOptionsCreator.setUserId(selectedUserId);

                            GWT_MFA_CREDENTIAL_OPTIONS_SERVICE.create(xsrfToken, gwtMfaCredentialOptionsCreator, selfManagement, new AsyncCallback<GwtMfaCredentialOptions>() {

                                @Override
                                public void onFailure(Throwable ex) {
                                    FailureHandler.handle(ex);
                                    doUnmask();
                                }

                                @Override
                                public void onSuccess(final GwtMfaCredentialOptions mfaCredentialOptions) {
                                    keyEnabled = true;
                                    // TODO Scratch Codes
                                    try {
                                        TwoFAManagementPanel.this.gwtMfaCredentialOptions = mfaCredentialOptions;
                                        Date date = new Date();

                                        StringBuilder sb = new StringBuilder("/image/2FAQRcode?");

                                        sb.append("username=")
                                                .append(selectedUserName)
                                                .append("&accountName=")
                                                .append(currentSession.getSelectedAccountName())
                                                .append("&key=")
                                                .append(mfaCredentialOptions.getAuthenticationKey())
                                                .append("&timestamp=")
                                                .append(date.getTime());
                                        barcodeImage.setUrl(sb.toString());
                                    } catch (Exception e) {
                                        FailureHandler.handle(e);
                                        doUnmask();
                                    }
                                }
                            });

                        } else {
                            doMask(MSGS.maskDisableTwoFA());
                            GWT_MFA_CREDENTIAL_OPTIONS_SERVICE.delete(xsrfToken, currentSession.getSelectedAccountId(), gwtMfaCredentialOptions.getId(), selfManagement, new AsyncCallback<Void>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                    doUnmask();
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    TwoFAManagementPanel.this.gwtMfaCredentialOptions = null;
                                    barcodeImage.setVisible(false);
                                    scratchCodesArea.setVisible(false);

                                    updateUIComponents(TwoFAManagementPanel.this.gwtMfaCredentialOptions);
                                    keyEnabled = false;
                                    CookieUtils.removeCookie(CookieUtils.KAPUA_COOKIE_TRUST + selectedUserName);
                                    doUnmask();
                                }
                            });
                        }
                    }
                });
            }
        });

        // Forget Trust Machine button
        forgetTrustedMachine = new KapuaButton(MSGS.mfaFormRevokeTrustedMachine(), new KapuaIcon(IconSet.LOCK), new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent myce) {
                final ForgetTrustMachineDialog dialog = new ForgetTrustMachineDialog(selectedUserName, currentSession.getSelectedAccountId(), gwtMfaCredentialOptions.getId(), selfManagement);
                dialog.addListener(Events.Hide, new Listener<WindowEvent>() {

                    @Override
                    public void handleEvent(WindowEvent be) {
                        if (be.getButtonClicked().equals(dialog.getButtonById(Dialog.YES))) {
                            GWT_MFA_CREDENTIAL_OPTIONS_SERVICE.find(currentSession.getSelectedAccountId(), gwtMfaCredentialOptions.getId(), selfManagement, new AsyncCallback<GwtMfaCredentialOptions>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    FailureHandler.handle(caught);
                                }

                                @Override
                                public void onSuccess(GwtMfaCredentialOptions result) {
                                    gwtMfaCredentialOptions = result;
                                    updateUIComponents(gwtMfaCredentialOptions);
                                }
                            });
                        }
                    }
                });
                dialog.show();
            }

        });

        help = new KapuaButton(MSGS.mfaFormHelp(), new KapuaIcon(IconSet.QUESTION), new SelectionListener<ButtonEvent>() {

            @Override
            public void componentSelected(ButtonEvent ce) {
                toggleHelp = !toggleHelp;
                if (toggleHelp) {
                    showHelp();
                    help.setText("Hide help");
                } else {
                    hideHelp();
                    help.setText("Help");
                }
            }
        });

        menuToolBar.add(enable2FA);
        menuToolBar.add(new SeparatorToolItem());
        menuToolBar.add(forgetTrustedMachine);
        menuToolBar.add(new SeparatorToolItem());
        menuToolBar.add(help);

        return menuToolBar;
    }

    private void buildHelpPanel() {
        formData = new FormData("-30");

        helpPanel = new com.extjs.gxt.ui.client.widget.form.FormPanel();
        helpPanel.setFrame(false);
        helpPanel.setBodyBorder(false);
        helpPanel.setHeaderVisible(false);
        helpPanel.setAutoHeight(true);

        // Explanation header
        FieldSet helpFieldSet = new FieldSet();
        helpFieldSet.setBorders(true);
        helpFieldSet.setHeading(MSGS.mfaHeaderHelp());

        LabelField text3 = new LabelField();
        text3.setText(MSGS.mfaFormBarcodeLabel3());
        text3.setName("explanation3");

        LabelField text4 = new LabelField();
        text4.setText(MSGS.mfaFormBarcodeLabel4());
        text4.setName("explanation4");

        FormLayout iconsLayout = new FormLayout();

        FieldSet iconsFieldSet = new FieldSet();
        iconsFieldSet.setBorders(false);
        iconsFieldSet.setLayout(iconsLayout);

        // images
        androidImage = new Image("img/2fa/android.png");
        blankImage = new Image("img/2fa/blank.png");
        appleImage = new Image("img/2fa/apple.png");

        appleImage.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                com.google.gwt.user.client.Window.open("https://itunes.apple.com/us/app/google-authenticator/id388497605?mt=8&uo=4", "_blank", "");
            }
        });

        appleImage.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                appleImage.getElement().getStyle().setCursor(Cursor.POINTER);
            }
        });

        androidImage.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                com.google.gwt.user.client.Window.open("https://play.google.com/store/apps/details?id=com.google.android.apps.authenticator2", "_blank", "");
            }
        });

        androidImage.addMouseOverHandler(new MouseOverHandler() {

            @Override
            public void onMouseOver(MouseOverEvent event) {
                androidImage.getElement().getStyle().setCursor(Cursor.POINTER);
            }
        });

        iconsFieldSet.add(androidImage);
        iconsFieldSet.add(blankImage);
        iconsFieldSet.add(appleImage);

        HorizontalPanel iconsPanel = new HorizontalPanel();
        iconsPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        iconsPanel.add(iconsFieldSet);

        //
        LabelField text5 = new LabelField(MSGS.mfaFormBarcodeLabel5());
        text5.setName("explanation5");

        LabelField text6 = new LabelField(MSGS.mfaFormBarcodeLabel6());
        text6.setName("explanation6");

        LabelField text7 = new LabelField(MSGS.mfaFormBarcodeLabel7());
        text7.setName("explanation7");

        //
        helpFieldSet.add(text3, formData);
        helpFieldSet.add(text4, formData);

        helpFieldSet.add(iconsPanel, formData);

        helpFieldSet.add(text5, formData);
        helpFieldSet.add(text6, formData);
        helpFieldSet.add(text7, formData);

        helpPanel.add(helpFieldSet);
    }

    private void showHelp() {
        helpPanel.show();
    }

    private void hideHelp() {
        helpPanel.hide();
    }

    private void printScratchCodes(List<GwtScratchCode> scratchCodes) {

        StringBuilder sb = new StringBuilder();

        sb.append("Scratch Codes:<br/><br/>");

        for (GwtScratchCode code : scratchCodes) {
            sb.append(code.getScratchCode()).append("</br>");
        }

        scratchCodesArea.setText(sb.toString());
    }

    @Override
    protected void onRender(Element parent, int pos) {
        super.onRender(parent, pos);

        twoFAContentPanel = new ContentPanel();
        twoFAContentPanel.setBorders(false);
        twoFAContentPanel.setBodyBorder(false);
        twoFAContentPanel.setHeaderVisible(false);

        // Add Help
        buildHelpPanel();
        twoFAContentPanel.add(helpPanel);
        hideHelp();

        // Explanation header
        FormLayout explanationLayout2 = new FormLayout();

        FieldSet explanationHeader = new FieldSet();
        explanationHeader.setBorders(false);
        explanationHeader.setAutoWidth(true);
        explanationHeader.setLayout(explanationLayout2);

        final Text text = new Text();
        text.setText(MSGS.mfaFormBarcodeLabel());

        explanationHeader.add(text);

        final Text text2 = new Text();
        text2.setText(MSGS.mfaFormBarcodeLabel2());
        explanationHeader.add(text2);

        explanationHeader.add(new HTML("<br />"));
        explanationHeader.add(new HTML("<br />"));

        enabledText = new Text();
        explanationHeader.add(enabledText);

        twoFAContentPanel.add(explanationHeader);

        //
        // QR Code Image
        barcodeImage = new Image();
        barcodeImage.addLoadHandler(new LoadHandler() {

            @Override
            public void onLoad(LoadEvent event) {
                keyEnabled = true;
                barcodeImage.setVisible(true);

                GWT_XSRF_SERVICE.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        FailureHandler.handle(caught);
                        doUnmask();
                    }

                    @Override
                    public void onSuccess(final GwtXSRFToken newXsrfToken) {
                        GWT_SCRATCH_CODES_SERVICE.createScratchCodes(newXsrfToken, currentSession.getSelectedAccountId(), gwtMfaCredentialOptions.getId(), selfManagement, new AsyncCallback<List<GwtScratchCode>>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                FailureHandler.handle(caught);
                                doUnmask();
                            }

                            @Override
                            public void onSuccess(List<GwtScratchCode> result) {
                                scratchCodesArea.setVisible(true);
                                printScratchCodes(result);
                                updateUIComponents(gwtMfaCredentialOptions);
                                getButtonBar().enable();
                                doUnmask();
                            }

                        });
                    }

                });
            }

        });
        //
        // Scratch Codes Text Area
        scratchCodesArea = new Text();
        scratchCodesArea.setStyleAttribute("padding", "10px 0px 0px 5px");
        scratchCodesArea.setStyleAttribute("text-align", "center");
        scratchCodesArea.setStyleName("x-form-label");
        scratchCodesArea.setWidth(110);

        HorizontalPanel imagePanel = new HorizontalPanel();
        imagePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        imagePanel.add(barcodeImage);
        imagePanel.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
        imagePanel.add(scratchCodesArea);

        Status internalStatus = new Status();
        internalStatus.setBusy("MSGS.waitMsg()");
        internalStatus.hide();

        twoFAContentPanel.setButtonAlign(HorizontalAlignment.LEFT);
        twoFAContentPanel.getButtonBar().add(internalStatus);
        twoFAContentPanel.getButtonBar().add(new FillToolItem());

        twoFAContentPanel.add(imagePanel, formData);

        twoFAContentPanel.setButtonAlign(HorizontalAlignment.CENTER);
        twoFAContentPanel.setTopComponent(createToolBar());

        add(twoFAContentPanel);

        doMask(MSGS.maskTwoFAManagementPanel());
        GWT_MFA_CREDENTIAL_OPTIONS_SERVICE.findByUserId(currentSession.getSelectedAccountId(), selectedUserId, selfManagement, new AsyncCallback<GwtMfaCredentialOptions>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught);
                doUnmask();
            }

            @Override
            public void onSuccess(GwtMfaCredentialOptions gwtMfaCredentialOptions) {
                TwoFAManagementPanel.this.gwtMfaCredentialOptions = gwtMfaCredentialOptions;
                updateUIComponents(gwtMfaCredentialOptions);
                doUnmask();
            }
        });

    }

    private void updateUIComponents(GwtMfaCredentialOptions gwtMfaCredentialOptions) {
        boolean twoFactorAuth = gwtMfaCredentialOptions != null && gwtMfaCredentialOptions.getAuthenticationKey() != null;
        boolean hasCredentialWrite = currentSession.hasPermission(CredentialSessionPermission.write());

        // Always enabled for self management (both by being in the standalone dialog or clicking the user in the tab,
        // otherwise only allow to disable 2FA if user has credential write
        enable2FA.setEnabled(selfManagement || (selectedUserName.equals(currentSession.getUserName())) || (hasCredentialWrite && gwtMfaCredentialOptions != null));
        forgetTrustedMachine.setEnabled(twoFactorAuth && (hasCredentialWrite || selfManagement) && gwtMfaCredentialOptions.getTrustKey() != null);

        // 2FA button icon & label
        if (twoFactorAuth) {
            enable2FA.setText(MSGS.mfaButtonDisable());
            enabledText.setText(MSGS.twoFAEnabled(selectedUserName));
        } else {
            enable2FA.setText(MSGS.mfaButtonEnable());
            enabledText.setText(MSGS.twoFADisabled(selectedUserName));
        }
        layout(true);
    }

    private void doMask(String message) {
        if (selfManagement) {
            selfManagementDialog.mask(message);
        } else {
            mask(MSGS.maskEnableTwoFA());
        }
    }

    private void doUnmask() {
        if (selfManagement) {
            selfManagementDialog.unmask();
        } else {
            unmask();
        }
    }

}
