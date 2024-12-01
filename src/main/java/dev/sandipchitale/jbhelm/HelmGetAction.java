package dev.sandipchitale.jbhelm;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class HelmGetAction extends HelmExplorerAbstractAction {
    private final JBList<NamespaceSecretReleaseRevision> namespaceSecretReleaseRevisionList = new JBList<>();
    private final WhatPanel whatPanel = WhatPanel.build();

    public HelmGetAction() {
        namespaceSecretReleaseRevisionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        namespaceSecretReleaseRevisionList.setCellRenderer(ReleaseRevisionNamespaceDefaultListCellRenderer.INSTANCE);

        whatPanel.add(new JScrollPane(namespaceSecretReleaseRevisionList), BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        DefaultMutableTreeNode selectedNode = helmExplorerToolWindow.getSelectedNode();
        if (selectedNode != null) {
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof HelmExplorerToolWindow.SecretNode secretNode) {
                Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet =
                        new HashSet<>();

                namespaceStringStringNamespaceSecretReleaseRevisionSet.add(new NamespaceSecretReleaseRevision(
                        secretNode.namespace(),
                        secretNode.secret(),
                        secretNode.release(),
                        secretNode.revision()
                ));

                namespaceSecretReleaseRevisionList.setModel(JBList.createDefaultListModel(namespaceStringStringNamespaceSecretReleaseRevisionSet));
                namespaceSecretReleaseRevisionList.setSelectedIndex(0);

                DialogBuilder builder = new DialogBuilder(anActionEvent.getProject());
                builder.setCenterPanel(whatPanel);
                builder.setDimensionServiceKey("SelectNamespaceHelmReleaseRevision");
                builder.setTitle("Select Helm Release.Revision [ Namespace ]");
                builder.removeAllActions();

                builder.addCancelAction();

                builder.addOkAction();
                builder.setOkActionEnabled(namespaceSecretReleaseRevisionList.getSelectedIndex() != -1);

                builder.setOkOperation(() -> {
                    if (whatPanel.isAny()) {
                        builder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
                    } else {
                        Messages.showMessageDialog(
                                anActionEvent.getProject(),
                                "Please select at least one of chart info, values, templates, manifests, hooks, notes for get",
                                "Select at Least One for Get",
                                Messages.getInformationIcon());
                    }
                });

                ListSelectionListener adjustOkActionState = (ListSelectionEvent listSelectionEvent) ->
                        builder.setOkActionEnabled(namespaceSecretReleaseRevisionList.getSelectedIndex() != -1);

                try {
                    namespaceSecretReleaseRevisionList.addListSelectionListener(adjustOkActionState);
                    boolean isOk = builder.show() == DialogWrapper.OK_EXIT_CODE;
                    if (isOk) {
                        if (whatPanel.isAny()) {
                            NamespaceSecretReleaseRevision selectedValue = namespaceSecretReleaseRevisionList.getSelectedValue();
                            if (selectedValue != null) {
                                Utils.showReleaseRevision(anActionEvent.getProject(), selectedValue, whatPanel);
                            }
                        }
                    }
                } finally {
                    // Remove listener
                    namespaceSecretReleaseRevisionList.removeListSelectionListener(adjustOkActionState);
                }
            }
        }

    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setVisible(true);
        boolean enabled = false;
        DefaultMutableTreeNode selectedNode = helmExplorerToolWindow.getSelectedNode();
        if (selectedNode != null) {
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof HelmExplorerToolWindow.SecretNode secretNode) {
                enabled = true;
            }
        }
        e.getPresentation().setEnabled(enabled);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

}
