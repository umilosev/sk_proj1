package provider;

import api.exception.RepositoryException;
import api.provider.RepositoryStorageProvider;
import config.RepositoryConfig;
import model.Dependency;
import model.artifact.Artifact;
import model.artifact.ArtifactMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LocalRepositoryStorageProvider implements RepositoryStorageProvider {

    private static final String SCHEME = "local";
    private static final String CONFIG_FILE_NAME = "repo-config.properties";

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public void initializeRepository(String repoPath, RepositoryConfig config) throws RepositoryException {
        try {
            Path root = Paths.get(repoPath);
            if (!Files.exists(root)) {
                Files.createDirectories(root);
            }

            Path configPath = root.resolve(CONFIG_FILE_NAME);
            Properties props = new Properties();
            props.setProperty("allowedExtensions", String.join(",", config.getAllowedExtensions()));

            StringBuilder scopeBuilder = new StringBuilder();
            config.getSupportedScopes().forEach((scope, transitive) ->
                    scopeBuilder.append(scope).append(":").append(transitive).append(";")
            );
            props.setProperty("supportedScopes", scopeBuilder.toString());

            try (OutputStream os = Files.newOutputStream(configPath)) {
                props.store(os, "MiniMaven Repository Configuration");
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to initialize local repository at: " + repoPath, e);
        }
    }

    @Override
    public RepositoryConfig getConfig(String repoPath) throws RepositoryException {
        Path configPath = Paths.get(repoPath, CONFIG_FILE_NAME);
        if (!Files.exists(configPath)) {
            throw new RepositoryException("Repository is not initialized. Missing " + CONFIG_FILE_NAME);
        }

        try {
            Properties props = new Properties();
            try (InputStream is = Files.newInputStream(configPath)) {
                props.load(is);
            }

            String extStr = props.getProperty("allowedExtensions", "");
            Set<String> extensions = Arrays.stream(extStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());

            String scopeStr = props.getProperty("supportedScopes", "");
            Map<String, Boolean> scopes = new HashMap<>();
            if (!scopeStr.isEmpty()) {
                for (String entry : scopeStr.split(";")) {
                    if (entry.contains(":")) {
                        String[] parts = entry.split(":");
                        scopes.put(parts[0].trim(), Boolean.parseBoolean(parts[1].trim()));
                    }
                }
            }

            return new RepositoryConfig(extensions, scopes);
        } catch (IOException e) {
            throw new RepositoryException("Failed to read repository config from: " + repoPath, e);
        }
    }

    @Override
    public boolean artifactExists(String repoPath, Artifact artifact) throws RepositoryException {
        Path artifactDir = Paths.get(repoPath, artifact.toPath());
        return Files.exists(artifactDir) && Files.isDirectory(artifactDir);
    }







    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;

        int start = idx + pattern.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }

        int end = start;
        while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '\n' && json.charAt(end) != '}') {
            end++;
        }

        return json.substring(start, end).trim();
    }
}
