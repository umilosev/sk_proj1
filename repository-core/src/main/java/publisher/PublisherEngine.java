package publisher;

import api.exception.RepositoryException;
import api.provider.RepositoryStorageProvider;
import config.RepositoryConfig;
import model.Dependency;
import model.artifact.Artifact;
import model.artifact.ArtifactMetadata;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class PublisherEngine {
    private final RepositoryStorageProvider provider;

    public PublisherEngine(RepositoryStorageProvider provider) {
        this.provider = provider;
    }

    public void publish(String repoPath, Artifact artifact, InputStream fileStream, ArtifactMetadata meta) throws RepositoryException {
        RepositoryConfig config = provider.getConfig(repoPath);

        // 1. Verify Extension
        if (!config.getAllowedExtensions().contains(meta.getExtension())) {
            throw new RepositoryException("File extension ." + meta.getExtension() + " is not permitted by repository configuration.");
        }

        // 2. Verify Duplicate Dependencies & Existence
        Set<String> depKeys = new HashSet<>();
        for (Dependency dep : meta.getDependencies()) {
            String depKey = dep.getArtifact().toString();
            if (!depKeys.add(depKey)) {
                throw new RepositoryException("Duplicate dependency detected: " + depKey);
            }

            if (!config.isScopeSupported(dep.getScope())) {
                throw new RepositoryException("Scope '" + dep.getScope() + "' is not supported by repository.");
            }

            // Ensure dependency exists in the repository
            if (!provider.artifactExists(repoPath, dep.getArtifact())) {
                throw new RepositoryException("Cannot publish: Required dependency " + depKey + " does not exist in repository.");
            }
        }

        // 3. Save component
        provider.publishArtifact(repoPath, artifact, fileStream, meta);
    }
}
