package dev.sandipchitale.jbhelm;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

public class HelmReleaseRevisionSecretsAccessor {
    static ApiClient apiClient;

    static {
        try {
            apiClient = Config.defaultClient();
            Configuration.setDefaultApiClient(apiClient);
        } catch (IOException ignore) {
            //
        }
    }

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetAllNamespaces() {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet = new LinkedHashSet<>();
        CoreV1Api api = new CoreV1Api();
        // Get the list of namespaces
        V1NamespaceList namespaceList = null;
        try {
            namespaceList = api.listNamespace().execute();
            // Print the names of the namespaces
            for (V1Namespace namespace : namespaceList.getItems()) {
                V1ObjectMeta namespaceMetadata = namespace.getMetadata();
                if (namespaceMetadata != null) {
                    namespaceStringStringNamespaceSecretReleaseRevisionSet.addAll(
                            getNamespaceSecretReleaseRevisionSetInNamespace(namespaceMetadata.getName()));
                }
            }
        } catch (ApiException ignore) {
        }
        return namespaceStringStringNamespaceSecretReleaseRevisionSet;
    }

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetInNamespace(String namespace) {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet = new LinkedHashSet<>();
        CoreV1Api api = new CoreV1Api();
        try {
            V1SecretList secretList = api.listNamespacedSecret(namespace).execute();
            for (V1Secret secret : secretList.getItems()) {
                V1ObjectMeta secretMetadata = secret.getMetadata();
                if (secretMetadata != null) {
                    Matcher matcher = Constants.helmSecretNamePattern.matcher(Objects.requireNonNull(secretMetadata.getName()));
                    if (matcher.matches()) {
                        String release = matcher.group(1);
                        String revision = matcher.group(2);
                        namespaceStringStringNamespaceSecretReleaseRevisionSet.add(new NamespaceSecretReleaseRevision(namespace, secret, release, revision));
                    }
                }
            }
        } catch (ApiException ignore) {
        }

        return namespaceStringStringNamespaceSecretReleaseRevisionSet;
    }

    static Set<NamespaceSecretReleaseRevision> getNamespaceSecretReleaseRevisionSetFromV1Secret(V1Secret secret) {
        Set<NamespaceSecretReleaseRevision> namespaceStringStringNamespaceSecretReleaseRevisionSet = new LinkedHashSet<>();
        Matcher matcher = Constants.helmSecretNamePattern.matcher(Objects.requireNonNull(Objects.requireNonNull(secret.getMetadata()).getName()));
        if (matcher.matches()) {
            String release = matcher.group(1);
            String revision = matcher.group(2);
            namespaceStringStringNamespaceSecretReleaseRevisionSet.add(new NamespaceSecretReleaseRevision(
                    secret.getMetadata().getNamespace(),
                    secret,
                    release,
                    revision));
        }
        return namespaceStringStringNamespaceSecretReleaseRevisionSet;
    }
}