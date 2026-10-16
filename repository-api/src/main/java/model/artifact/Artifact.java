package model.artifact;

import lombok.Getter;

import java.util.Objects;

@Getter
public class Artifact {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public Artifact(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            throw new IllegalArgumentException("Artifact coordinates cannot be null.");
        }
        this.groupId = groupId.trim();
        this.artifactId = artifactId.trim();
        this.version = version.trim();
    }

    public static Artifact parse(String coordinate) {
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Coordinate must be in format 'groupId:artifactId:version'. Received: " + coordinate);
        }
        return new Artifact(parts[0], parts[1], parts[2]);
    }

    public String toPath() {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version;
    }

    public String getFileName(String extension) {
        return artifactId + "-" + version + "." + extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artifact)) return false;
        Artifact artifact = (Artifact) o;
        return groupId.equals(artifact.groupId) &&
                artifactId.equals(artifact.artifactId) &&
                version.equals(artifact.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
