package dev.sandipchitale.jbhelm;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;

public class HelmExplorerToolWindow extends SimpleToolWindowPanel {
    private final Project project;
    private final Tree helmTree;

    static record NamespaceNode(V1Namespace namespace, String name) {
    }

    static record SecretNode(V1Secret secret, String release, String revision) {
    }

    public HelmExplorerToolWindow(Project project) {
        super(true, true);
        this.project = project;

        helmTree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode("Helm", true)));

        HelmTreeCellRenderer helmTreeCellRenderer = new HelmTreeCellRenderer();
        helmTree.setCellRenderer(helmTreeCellRenderer);

        helmTree.setRootVisible(true);

        helmTree.getSelectionModel().setSelectionMode(DefaultTreeSelectionModel.SINGLE_TREE_SELECTION);

        helmTree.addTreeSelectionListener(tse -> {
            Object lastPathComponent = tse.getPath().getLastPathComponent();
            if (lastPathComponent instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
                Object userObject = defaultMutableTreeNode.getUserObject();
            }
        });

        setContent(ScrollPaneFactory.createScrollPane(helmTree));

        final ActionManager actionManager = ActionManager.getInstance();
        ToolWindowEx helmExplorer = (ToolWindowEx) ToolWindowManager.getInstance(project).getToolWindow("Helm Explorer");

        HelmGetAction helmGetAction = (HelmGetAction) actionManager.getAction("HelmGet");
        helmGetAction.setHelmExplorerToolWindow(this);

        RefreshHelmExplorerAction refreshHelmExplorerAction = (RefreshHelmExplorerAction) actionManager.getAction("RefreshHelmExplorer");
        refreshHelmExplorerAction.setHelmExplorerToolWindow(this);
        Objects.requireNonNull(helmExplorer).setTitleActions(java.util.List.of(helmGetAction, refreshHelmExplorerAction));

        loadHelmTree();
    }

    DefaultMutableTreeNode getSelectedNode() {
        TreePath selectionPath = helmTree.getSelectionModel().getSelectionPath();
        if (selectionPath != null) {
            return (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        }
        return null;
    }

    void loadHelmTree() {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ApplicationManager.getApplication().runReadAction(() -> {
                DefaultMutableTreeNode rootDefaultMutableTreeNode = (DefaultMutableTreeNode) helmTree.getModel().getRoot();
                rootDefaultMutableTreeNode.removeAllChildren();
                helmTree.setPaintBusy(true);
                Objects.requireNonNull(getContent()).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    ApiClient client = Config.defaultClient();
                    Configuration.setDefaultApiClient(client);
                    CoreV1Api api = new CoreV1Api();
                    // Get the list of namespaces
                    V1NamespaceList namespaceList = api.listNamespace().execute();
                    // Print the names of the namespaces
                    for (V1Namespace namespace : namespaceList.getItems()) {
                        V1ObjectMeta namespaceMetadata = namespace.getMetadata();
                        if (namespaceMetadata != null) {
                            DefaultMutableTreeNode namespaceDefaultMutableTreeNode = new DefaultMutableTreeNode(new NamespaceNode(namespace, namespaceMetadata.getName()));
                            rootDefaultMutableTreeNode.add(namespaceDefaultMutableTreeNode);
                            V1SecretList secretList = api.listNamespacedSecret(namespaceMetadata.getName()).execute();
                            // Print the names of the secrets
                            for (V1Secret secret : secretList.getItems()) {
                                V1ObjectMeta secretMetadata = secret.getMetadata();
                                if (secretMetadata != null) {
                                    Matcher matcher = Constants.helmSecretNamePattern.matcher(Objects.requireNonNull(secretMetadata.getName()));
                                    if (matcher.matches()) {
                                        DefaultMutableTreeNode secretDefaultMutableTreeNode =
                                                new DefaultMutableTreeNode(new SecretNode(secret, matcher.group(1), matcher.group(2)));
                                        namespaceDefaultMutableTreeNode.add(secretDefaultMutableTreeNode);
                                    }
                                }
                            }
                        }
                    }
                } catch (ApiException | IOException ignore) {
                    Notification notification = new Notification("helmExplorerNotificationGroup",
                            "Could not Helm releases",
                            "Could not get information about Helm releases from Kubernetes cluster.",
                            NotificationType.ERROR);
                    notification.notify(project);
                } finally {
                    helmTree.setPaintBusy(false);
                    getContent().setCursor(null);
                }
            });
        });
    }

    private static class HelmTreeCellRenderer extends ColoredTreeCellRenderer {
        @Override
        public void customizeCellRenderer(JTree tree, Object value, boolean sel, boolean expanded,
                                          boolean leaf, int row, boolean hasFocus) {
            setIcon(AllIcons.Nodes.Folder);
            if (value instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
                Object userObject = defaultMutableTreeNode.getUserObject();
                if (userObject instanceof String text) {
                    append(text);
                    setIcon(HelmIcons.helmExplorerIcon);
                } else if (userObject instanceof NamespaceNode namespaceNode) {
                    append(namespaceNode.name());
                    setIcon(AllIcons.Actions.GroupByModuleGroup);
                } else if (userObject instanceof SecretNode secretNode) {
                    append(String.format("%s:%s", secretNode.release(), secretNode.revision));
                    setIcon(AllIcons.Actions.ListFiles);
                }
            }
        }
    }
}