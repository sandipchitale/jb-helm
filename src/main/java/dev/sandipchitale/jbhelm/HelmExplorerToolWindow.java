package dev.sandipchitale.jbhelm;

import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

public class HelmExplorerToolWindow extends SimpleToolWindowPanel {
    private final Project project;
    private final Tree helmTree;

    static record ContextNode(String name, Object context, boolean current) {
    }

    static record NamespaceNode(V1Namespace namespace, String name) {
    }

    static record SecretNode(V1Secret secret, String namespace, String release, String revision) {
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

        HelmDiffAction helmDiffAction = (HelmDiffAction) actionManager.getAction("HelmDiff");

        RefreshHelmExplorerAction refreshHelmExplorerAction = (RefreshHelmExplorerAction) actionManager.getAction("RefreshHelmExplorer");
        refreshHelmExplorerAction.setHelmExplorerToolWindow(this);
        Objects.requireNonNull(helmExplorer).setTitleActions(java.util.List.of(helmGetAction, helmDiffAction, refreshHelmExplorerAction));

        JPopupMenu helmReleasesPopupMenu = new JPopupMenu();
        JMenuItem helmReleasesHelmGetMenuItem = new JMenuItem("Helm get...");
        helmReleasesHelmGetMenuItem.addActionListener((ActionEvent actionEvent) -> {
            actionManager.tryToExecute(
                    helmGetAction,
                    null,
                    helmTree,
                    "Helm Explorer",
                    true
            );
        });
        helmReleasesPopupMenu.add(helmReleasesHelmGetMenuItem);

        helmTree.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                showPopup(mouseEvent);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    Tree helmTree = (Tree) e.getSource();
                    TreePath closestPathForLocation = helmTree.getClosestPathForLocation(e.getPoint().x, e.getPoint().y);
                    if (closestPathForLocation != null) {
                        Object lastPathComponent = closestPathForLocation.getLastPathComponent();
                        if (lastPathComponent instanceof DefaultMutableTreeNode defaultMutableTreeNode) {
                            Object userObject = defaultMutableTreeNode.getUserObject();
                            if (userObject instanceof SecretNode secretNode) {
                                helmReleasesPopupMenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                    }
                }
            }
        });

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
                    try {
                        KubeConfig kubeConfig = KubeConfig.loadKubeConfig(
                                new FileReader(
                                        Path.of(System.getProperty("user.home"),
                                                KubeConfig.KUBEDIR,
                                                KubeConfig.KUBECONFIG).toFile()));
                        String currentContextName = kubeConfig.getCurrentContext();
                        DefaultMutableTreeNode currentContextNode = null;
                        ArrayList<Object> contexts = kubeConfig.getContexts();
                        for (Object context : contexts) {
                            if (context instanceof Map<?, ?> contextMap) {
                                Object nameObject = contextMap.get("name");
                                if (nameObject instanceof String name) {
                                    DefaultMutableTreeNode contextNode =
                                            new DefaultMutableTreeNode(new ContextNode(name,
                                                    contextMap.get("context"),
                                                    currentContextName.equals(name)));
                                    if (currentContextName.equals(name)) {
                                        currentContextNode = contextNode;
                                    }
                                    rootDefaultMutableTreeNode.add(contextNode);
                                }
                            }
                        }
                        if (currentContextNode != null) {
                            rootDefaultMutableTreeNode = currentContextNode;
                        }
                    } catch (Exception ignore) {
                        // Oh well
                    }

                    ApiClient apiClient = Config.defaultClient();
                    Configuration.setDefaultApiClient(apiClient);
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
                                                new DefaultMutableTreeNode(new SecretNode(secret,
                                                        secretMetadata.getNamespace(),
                                                        matcher.group(1),
                                                        matcher.group(2)));
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
                    TreeUtil.expandAll(helmTree);
                    ((DefaultTreeModel) helmTree.getModel()).nodeStructureChanged((DefaultMutableTreeNode) helmTree.getModel().getRoot());
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
                } else if (userObject instanceof ContextNode contextNode) {
                    append(contextNode.name());
                    setIcon(AllIcons.Actions.GroupByClass);
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