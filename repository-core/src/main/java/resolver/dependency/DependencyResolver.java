package resolver.dependency;

import api.exception.RepositoryException;
import api.provider.RepositoryStorageProvider;
import config.RepositoryConfig;
import model.Dependency;
import model.artifact.Artifact;
import model.artifact.ArtifactMetadata;
import org.w3c.dom.*;
import resolver.conflict.ConflictResolver;

import java.util.*;

public class DependencyResolver {
    private final RepositoryStorageProvider storage;
    private final String repoPath;
    private final ConflictResolver conflictResolver;

    public DependencyResolver(RepositoryStorageProvider storage, String repoPath, ConflictResolver conflictResolver) {
        this.storage = storage;
        this.repoPath = repoPath;
        this.conflictResolver = conflictResolver;
    }

    public Set<Artifact> resolveDependencies(List<Artifact> directArtifacts) throws RepositoryException {
        RepositoryConfig config = storage.getConfig(repoPath);

        // Group artifacts by "groupId:artifactId" to detect conflicts
        Map<String, List<Artifact>> discoveredVersions = new HashMap<>();
        Map<Artifact, Integer> artifactDepths = new HashMap<>();

        Queue<Node> queue = new LinkedList<>();

        for (Artifact direct : directArtifacts) {
            queue.add(new Node(direct, 0, null, false));
        }

        while (!queue.isEmpty()) {
            //mapping all of the available versions
            Node current = queue.poll();
            Artifact artifact = current.artifact;
            String key = artifact.getGroupId() + ":" + artifact.getArtifactId();

            discoveredVersions.computeIfAbsent(key, k -> new ArrayList<>()).add(artifact);
            artifactDepths.put(artifact, current.depth);

            ArtifactMetadata meta = storage.fetchMetadata(repoPath, artifact);

            for (Dependency dep : meta.getDependencies()) {
                // Rule 1: Skip optional dependencies when resolving transitively (depth > 0)
                if (current.depth > 0 && dep.isOptional()) {
                    continue;
                }

                // Rule 2: Scope Check (Check if scope is supported and transitive)
                if (!config.isScopeSupported(dep.getScope())) {
                    throw new RepositoryException("Unsupported scope: " + dep.getScope());
                }

                boolean isTransitive = config.isScopeTransitive(dep.getScope());
                if (current.depth == 0 || isTransitive) {
                    queue.add(new Node(dep.getArtifact(), current.depth + 1, dep.getScope(), dep.isOptional()));
                }
            }
        }

        // Apply Conflict Resolution strategy per component
        Set<Artifact> resolvedFinalSet = new HashSet<>();
        for (List<Artifact> candidates : discoveredVersions.values()) {
            if (candidates.size() == 1) {
                resolvedFinalSet.add(candidates.get(0));
            } else {
                Artifact winningArtifact = conflictResolver.resolve(candidates, artifactDepths);
                resolvedFinalSet.add(winningArtifact);
            }
        }

        return resolvedFinalSet;
    }

    private static class Node {
        final Artifact artifact;
        final int depth;
        final String scope;
        final boolean optional;

        Node(Artifact artifact, int depth, String scope, boolean optional) {
            this.artifact = artifact;
            this.depth = depth;
            this.scope = scope;
            this.optional = optional;
        }
    }
}
