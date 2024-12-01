package dev.sandipchitale.jbhelm;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class RefreshHelmExplorerAction extends AnAction {
    private HelmExplorerToolWindow helmExplorerToolWindow;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (helmExplorerToolWindow != null) {
            helmExplorerToolWindow.loadHelmTree();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(true);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    public void setHelmExplorerToolWindow(HelmExplorerToolWindow helmExplorerToolWindow) {
        this.helmExplorerToolWindow = helmExplorerToolWindow;
    }
}
