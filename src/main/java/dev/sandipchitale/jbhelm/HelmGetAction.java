package dev.sandipchitale.jbhelm;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;

public class HelmGetAction extends HelmExplorerAbstractAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DefaultMutableTreeNode selectedNode = helmExplorerToolWindow.getSelectedNode();
        if (selectedNode != null) {
            Object userObject = selectedNode.getUserObject();
            if (userObject instanceof HelmExplorerToolWindow.SecretNode secretNode) {
                Notification notification = new Notification("helmExplorerNotificationGroup",
                        "Helm get",
                        String.format("Helm get %s:%s", secretNode.release(), secretNode.revision()),
                        NotificationType.INFORMATION);
                notification.notify(e.getProject());
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
