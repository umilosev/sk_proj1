package api;

import api.artifact.Artifact;
import lombok.Getter;

@Getter
public class Dependency {
    private final Artifact artifact;
    private final String scope;
    private final boolean optional;

    public Dependency(Artifact artifact, String scope, boolean optional) {
        this.artifact = artifact;
        this.scope = scope;
        this.optional = optional;
    }
}
