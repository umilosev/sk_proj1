package app;

import api.provider.RepositoryStorageProvider;
import config.RepositoryConfig;
import factory.RepositoryStorageFactory;
import model.Dependency;
import model.artifact.Artifact;
import model.artifact.ArtifactMetadata;
import publisher.PublisherEngine;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PublisherApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String command = args[0];
        try {
            switch (command.toLowerCase()) {
                case "init":
                    handleInit(args);
                    break;
                case "publish":
                    handlePublish(args);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
            }
        } catch (Exception e) {
            System.err.println("Operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void handleInit(String[] args) throws Exception {
        // Usage: init --driver local --repo /path/to/repo --ext jar,zip --scopes compile:true,test:false
        Map<String, String> flags = parseFlags(args);
        String driverScheme = flags.getOrDefault("driver", "local");
        String repoPath = flags.get("repo");
        String extStr = flags.getOrDefault("ext", "jar");
        String scopesStr = flags.getOrDefault("scopes", "compile:true,test:false");

        if (repoPath == null) {
            throw new IllegalArgumentException("Missing required flag: --repo <path>");
        }

        Set<String> extensions = new HashSet<>(Arrays.asList(extStr.split(",")));
        Map<String, Boolean> scopes = new HashMap<>();
        for (String pair : scopesStr.split(",")) {
            String[] parts = pair.split(":");
            scopes.put(parts[0].trim(), Boolean.parseBoolean(parts[1].trim()));
        }

        RepositoryConfig config = new RepositoryConfig(extensions, scopes);
        RepositoryStorageProvider provider = RepositoryStorageFactory.getProvider(driverScheme);
        provider.initializeRepository(repoPath, config);

        System.out.println("Successfully initialized " + driverScheme + " repository at: " + repoPath);
    }

    private static void handlePublish(String[] args) throws Exception {
        // Usage: publish --driver local --repo /path/to/repo --coordinate com.example:lib-a:1.0.0 --file /path/to/file.jar --ext jar --deps "com.example:lib-b:1.0.0:compile:false"
        Map<String, String> flags = parseFlags(args);
        String driverScheme = flags.getOrDefault("driver", "local");
        String repoPath = flags.get("repo");
        String coord = flags.get("coordinate");
        String filePath = flags.get("file");
        String ext = flags.getOrDefault("ext", "jar");
        String depsStr = flags.get("deps"); // Format: "group:art:ver:scope:optional;group2:art2:ver2:scope:optional"

        if (repoPath == null || coord == null || filePath == null) {
            throw new IllegalArgumentException("Missing required flags. --repo, --coordinate, and --file are mandatory.");
        }

        Artifact artifact = Artifact.parse(coord);
        List<Dependency> dependencies = new ArrayList<>();

        if (depsStr != null && !depsStr.isBlank()) {
            for (String depRaw : depsStr.split(";")) {
                String[] parts = depRaw.split(":");
                if (parts.length < 5) {
                    throw new IllegalArgumentException("Invalid dependency format: " + depRaw + ". Use group:artifact:version:scope:optional");
                }
                Artifact depArt = new Artifact(parts[0], parts[1], parts[2]);
                String scope = parts[3];
                boolean optional = Boolean.parseBoolean(parts[4]);
                dependencies.add(new Dependency(depArt, scope, optional));
            }
        }

        ArtifactMetadata metadata = new ArtifactMetadata(artifact, ext, dependencies);
        RepositoryStorageProvider provider = RepositoryStorageFactory.getProvider(driverScheme);
        PublisherEngine engine = new PublisherEngine(provider);

        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            engine.publish(repoPath, artifact, is, metadata);
        }

        System.out.println("Successfully published " + artifact + " to repository.");
    }

    private static Map<String, String> parseFlags(String[] args) {
        Map<String, String> flags = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--") && i + 1 < args.length) {
                flags.put(args[i].substring(2), args[i + 1]);
                i++;
            }
        }
        return flags;
    }

    private static void printUsage() {
        System.out.println("MiniMaven Publisher CLI");
        System.out.println("Commands:");
        System.out.println("  init    --driver <scheme> --repo <path> --ext <ext1,ext2> --scopes <scope:transitive,...>");
        System.out.println("  publish --driver <scheme> --repo <path> --coordinate <g:a:v> --file <path> --ext <ext> [--deps <g:a:v:scope:optional;...>]");
    }
}
