package provider;

import api.exception.RepositoryException;
import api.provider.RepositoryStorageProvider;
import config.RepositoryConfig;
import model.artifact.Artifact;
import model.artifact.ArtifactMetadata;

import java.io.InputStream;
import java.util.List;

public class NetworkRepositoryStorageProvider implements RepositoryStorageProvider {

    @Override
    public String getScheme() {
        return "";
    }

    @Override
    public void initializeRepository(String repoPath, RepositoryConfig config) throws RepositoryException {

    }

    @Override
    public RepositoryConfig getConfig(String repoPath) throws RepositoryException {
        return null;
    }

    @Override
    public boolean artifactExists(String repoPath, Artifact artifact) throws RepositoryException {
        return false;
    }

    @Override
    public void publishArtifact(String repoPath, Artifact artifact, InputStream fileStream, ArtifactMetadata metadata) throws RepositoryException {

    }

    @Override
    public InputStream fetchArtifact(String repoPath, Artifact artifact, String extension) throws RepositoryException {
        return null;
    }

    @Override
    public ArtifactMetadata fetchMetadata(String repoPath, Artifact artifact) throws RepositoryException {
        return null;
    }

    @Override
    public List<String> listProjects(String repoPath, String groupId) throws RepositoryException {
        return List.of();
    }

    @Override
    public List<String> listVersions(String repoPath, String groupId, String artifactId) throws RepositoryException {
        return List.of();
    }
}
