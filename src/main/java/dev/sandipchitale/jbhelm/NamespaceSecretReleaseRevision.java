package dev.sandipchitale.jbhelm;

import io.kubernetes.client.openapi.models.V1Secret;

public record NamespaceSecretReleaseRevision(String namespace, V1Secret v1Secret, String release, String revision) {
}
