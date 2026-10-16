package model.artifact;

import model.Dependency;
import lombok.Getter;

import java.util.List;

@Getter
public class ArtifactMetadata {
    private final Artifact artifact;
    private final String extension;
    private final List<Dependency> dependencies;

    public ArtifactMetadata(Artifact artifact, String extension, List<Dependency> dependencies) {
        this.artifact = artifact;
        this.extension = extension;
        this.dependencies = dependencies;
    }
}
