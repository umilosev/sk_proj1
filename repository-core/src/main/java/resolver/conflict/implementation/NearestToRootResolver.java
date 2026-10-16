package resolver.conflict.implementation;

import model.artifact.Artifact;
import resolver.conflict.ConflictResolver;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class NearestToRootResolver implements ConflictResolver {
    @Override
    public Artifact resolve(List<Artifact> candidates, Map<Artifact, Integer> depthMap) {
        return candidates.stream()
                .min(Comparator.comparingInt(depthMap::get))
                .orElseThrow();
    }
}
