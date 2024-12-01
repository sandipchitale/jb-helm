package dev.sandipchitale.jbhelm;

import com.intellij.openapi.actionSystem.AnAction;

public abstract class HelmExplorerAbstractAction extends AnAction {
    protected HelmExplorerToolWindow helmExplorerToolWindow;

    void setHelmExplorerToolWindow(HelmExplorerToolWindow helmExplorerToolWindow) {
        this.helmExplorerToolWindow = helmExplorerToolWindow;
    }
}
