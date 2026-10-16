package resolver.implementation;

import model.artifact.Artifact;
import resolver.ConflictResolver;

import java.util.List;
import java.util.Map;

public class NewestVersionResolver implements ConflictResolver {

    @Override
    public Artifact resolve(List<Artifact> candidates, Map<Artifact, Integer> depthMap) {
        return candidates.stream()
                .max((a1, a2) -> compareVersions(a1.getVersion(), a2.getVersion()))
                .orElseThrow();
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("\\D+", "")) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("\\D+", "")) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }
}
