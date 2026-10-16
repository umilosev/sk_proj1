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


    @Override
    public void publishArtifact(String repoPath, Artifact artifact, InputStream fileStream, ArtifactMetadata metadata) throws RepositoryException {
        try {
            Path targetDir = Paths.get(repoPath, artifact.toPath());
            Files.createDirectories(targetDir);

            // 1. Save artifact binary file
            String fileName = artifact.getFileName(metadata.getExtension());
            Path targetFile = targetDir.resolve(fileName);
            Files.copy(fileStream, targetFile, StandardCopyOption.REPLACE_EXISTING);

            // 2. Save metadata JSON
            Path metaFile = targetDir.resolve(artifact.getArtifactId() + "-" + artifact.getVersion() + ".json");
            String jsonContent = serializeMetadata(metadata);
            Files.writeString(metaFile, jsonContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        } catch (IOException e) {
            throw new RepositoryException("Failed to publish artifact: " + artifact, e);
        }
    }


    @Override
    public InputStream fetchArtifact(String repoPath, Artifact artifact, String extension) throws RepositoryException {
        Path filePath = Paths.get(repoPath, artifact.toPath(), artifact.getFileName(extension));
        if (!Files.exists(filePath)) {
            throw new RepositoryException("Artifact file not found: " + filePath);
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            throw new RepositoryException("Failed to read artifact file: " + filePath, e);
        }
    }


    @Override
    public ArtifactMetadata fetchMetadata(String repoPath, Artifact artifact) throws RepositoryException {
        Path metaFile = Paths.get(repoPath, artifact.toPath(), artifact.getArtifactId() + "-" + artifact.getVersion() + ".json");
        if (!Files.exists(metaFile)) {
            throw new RepositoryException("Metadata file not found for artifact: " + artifact);
        }

        try {
            String jsonStr = Files.readString(metaFile);
            return deserializeMetadata(jsonStr);
        } catch (IOException e) {
            throw new RepositoryException("Failed to read metadata for artifact: " + artifact, e);
        }
    }


    @Override
    public List<String> listProjects(String repoPath, String groupId) throws RepositoryException {
        Path groupPath = Paths.get(repoPath, groupId.replace('.', '/'));
        if (!Files.exists(groupPath)) return Collections.emptyList();

        try (Stream<Path> stream = Files.list(groupPath)) {
            return stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RepositoryException("Failed to list projects for groupId: " + groupId, e);
        }
    }

    @Override
    public List<String> listVersions(String repoPath, String groupId, String artifactId) throws RepositoryException {
        Path projectPath = Paths.get(repoPath, groupId.replace('.', '/'), artifactId);
        if (!Files.exists(projectPath)) return Collections.emptyList();

        try (Stream<Path> stream = Files.list(projectPath)) {
            return stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RepositoryException("Failed to list versions for project: " + artifactId, e);
        }
    }

    // Direct JSON serialization helper
    private String serializeMetadata(ArtifactMetadata meta) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"groupId\": \"").append(meta.getArtifact().getGroupId()).append("\",\n");
        sb.append("  \"artifactId\": \"").append(meta.getArtifact().getArtifactId()).append("\",\n");
        sb.append("  \"version\": \"").append(meta.getArtifact().getVersion()).append("\",\n");
        sb.append("  \"extension\": \"").append(meta.getExtension()).append("\",\n");
        sb.append("  \"dependencies\": [\n");

        List<Dependency> deps = meta.getDependencies();
        for (int i = 0; i < deps.size(); i++) {
            Dependency d = deps.get(i);
            sb.append("    {\n");
            sb.append("      \"groupId\": \"").append(d.getArtifact().getGroupId()).append("\",\n");
            sb.append("      \"artifactId\": \"").append(d.getArtifact().getArtifactId()).append("\",\n");
            sb.append("      \"version\": \"").append(d.getArtifact().getVersion()).append("\",\n");
            sb.append("      \"scope\": \"").append(d.getScope()).append("\",\n");
            sb.append("      \"optional\": ").append(d.isOptional()).append("\n");
            sb.append("    }").append(i < deps.size() - 1 ? "," : "").append("\n");
        }

        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    // Lightweight manual JSON parser
    private ArtifactMetadata deserializeMetadata(String json) {
        String groupId = extractValue(json, "groupId");
        String artifactId = extractValue(json, "artifactId");
        String version = extractValue(json, "version");
        String extension = extractValue(json, "extension");

        Artifact artifact = new Artifact(groupId, artifactId, version);
        List<Dependency> deps = new ArrayList<>();

        if (json.contains("\"dependencies\":")) {
            String depsArray = json.substring(json.indexOf("\"dependencies\":"));
            String[] blocks = depsArray.split("\\{");

            for (int i = 1; i < blocks.length; i++) {
                String block = blocks[i];
                String dGroup = extractValue(block, "groupId");
                String dArt = extractValue(block, "artifactId");
                String dVer = extractValue(block, "version");
                String dScope = extractValue(block, "scope");
                boolean dOpt = Boolean.parseBoolean(extractValue(block, "optional"));

                if (dGroup != null && dArt != null && dVer != null) {
                    deps.add(new Dependency(new Artifact(dGroup, dArt, dVer), dScope, dOpt));
                }
            }
        }

        return new ArtifactMetadata(artifact, extension, deps);
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
