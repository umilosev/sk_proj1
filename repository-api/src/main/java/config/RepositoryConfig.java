package config;

import lombok.Getter;

import java.util.Map;
import java.util.Set;

@Getter
public class RepositoryConfig {
    private final Set<String> allowedExtensions;
    // Maps scope name to transitive property (e.g., "compile" -> true, "provided" -> false)
    private final Map<String, Boolean> supportedScopes;

    public RepositoryConfig(Set<String> allowedExtensions, Map<String, Boolean> supportedScopes) {
        this.allowedExtensions = allowedExtensions;
        this.supportedScopes = supportedScopes;
    }

    public boolean isScopeTransitive(String scope) {
        return supportedScopes.getOrDefault(scope, false);
    }
    public boolean isScopeSupported(String scope) {
        return supportedScopes.containsKey(scope);
    }
}
