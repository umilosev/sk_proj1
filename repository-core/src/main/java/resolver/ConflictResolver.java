package resolver;

import model.artifact.Artifact;

import java.util.List;
import java.util.Map;

public interface ConflictResolver {
    Artifact resolve(List<Artifact> candidates, Map<Artifact, Integer> depthMap);
}
