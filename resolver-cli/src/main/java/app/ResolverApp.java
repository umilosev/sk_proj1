package app;

import api.provider.RepositoryStorageProvider;
import factory.RepositoryStorageFactory;
import model.artifact.Artifact;
import resolver.conflict.*;
import resolver.conflict.implementation.*;
import resolver.dependency.DependencyResolver;

import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

public class ResolverApp {

    public static void main(String[] args) {
        Map<String, String> flags = parseFlags(args);

        String driverScheme = flags.getOrDefault("driver", "local");
        String repoPath = flags.get("repo");
        String manifestPath = flags.get("manifest");
        String outDir = flags.getOrDefault("out", "./downloaded_libs");
        String strategyName = flags.getOrDefault("strategy", "nearest"); // "nearest" or "newest"

        if (repoPath == null || manifestPath == null) {
            System.err.println("Usage: resolver-cli --driver <scheme> --repo <path> --manifest <file.txt> [--out <dir>] [--strategy nearest|newest]");
            return;
        }

        try {
            // 1. Parse requested dependencies from manifest file
            List<String> lines = Files.readAllLines(Paths.get(manifestPath));
            List<Artifact> directArtifacts = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    directArtifacts.add(Artifact.parse(line));
                }
            }

            // 2. Choose Strategy
            ConflictResolver strategy;
            if ("newest".equalsIgnoreCase(strategyName)) {
                strategy = new NewestVersionResolver();
            } else {
                strategy = new NearestToRootResolver();
            }

            // 3. Resolve Graph
            RepositoryStorageProvider provider = RepositoryStorageFactory.getProvider(driverScheme);
            DependencyResolver resolver = new DependencyResolver(provider, repoPath, strategy);

            System.out.println("Resolving dependency graph using strategy: " + strategyName.toUpperCase() + "...");
            Set<Artifact> resolvedArtifacts = resolver.resolveDependencies(directArtifacts);

            System.out.println("\n--- Resolved Dependency Set (" + resolvedArtifacts.size() + " components) ---");
            Path targetFolder = Paths.get(outDir);
            Files.createDirectories(targetFolder);

            // 4. Download files to output directory
            for (Artifact artifact : resolvedArtifacts) {
                String ext = provider.fetchMetadata(repoPath, artifact).getExtension();
                try (InputStream is = provider.fetchArtifact(repoPath, artifact, ext)) {
                    Path dest = targetFolder.resolve(artifact.getFileName(ext));
                    Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println(" [PULLED] " + artifact + " -> " + dest);
                }
            }

            System.out.println("\nSuccessfully downloaded all libraries to: " + targetFolder.toAbsolutePath());

        } catch (Exception e) {
            System.err.println("Resolution failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, String> parseFlags(String[] args) {
        Map<String, String> flags = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                flags.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return flags;
    }
}
