package api.provider;

import api.artifact.Artifact;
import api.artifact.ArtifactMetadata;
import api.exception.RepositoryException;
import config.RepositoryConfig;

import java.io.InputStream;
import java.util.List;

public interface RepositoryStorageProvider {
    String getScheme();

    void initializeRepository(String repoPath, RepositoryConfig config) throws RepositoryException;

    RepositoryConfig getConfig(String repoPath) throws RepositoryException;

    boolean artifactExists(String repoPath, Artifact artifact) throws RepositoryException;

    void publishArtifact(String repoPath, Artifact artifact, InputStream fileStream, ArtifactMetadata metadata) throws RepositoryException;

    InputStream fetchArtifact(String repoPath, Artifact artifact, String extension) throws RepositoryException;

    ArtifactMetadata fetchMetadata(String repoPath, Artifact artifact) throws RepositoryException;

    // Search and discovery
    List<String> listProjects(String repoPath, String groupId) throws RepositoryException;

    List<String> listVersions(String repoPath, String groupId, String artifactId) throws RepositoryException;
}
