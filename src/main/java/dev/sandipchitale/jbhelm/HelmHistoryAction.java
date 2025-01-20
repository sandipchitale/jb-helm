package dev.sandipchitale.jbhelm;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;

public class HelmHistoryAction extends HelmExplorerAbstractAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Project project = anActionEvent.getProject();
        DefaultMutableTreeNode selectedNode = helmExplorerToolWindow.getSelectedNode();
        if (selectedNode != null) {
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof HelmExplorerToolWindow.SecretNode secretNode) {
                @NotNull TerminalWidget terminalWidget =
                        TerminalToolWindowManager.getInstance(Objects.requireNonNull(project)).createShellWidget(project.getBasePath(), "helm history", true, true);
                terminalWidget.sendCommandToExecute(String.format("helm history -n %s %s", secretNode.namespace(), secretNode.release()));
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
