package factory;

import api.provider.RepositoryStorageProvider;

import java.util.ServiceLoader;

public class RepositoryStorageFactory {
    public static RepositoryStorageProvider getProvider(String scheme) {
        ServiceLoader<RepositoryStorageProvider> loader = ServiceLoader.load(RepositoryStorageProvider.class);
        for (RepositoryStorageProvider provider : loader) {
            if (provider.getScheme().equalsIgnoreCase(scheme)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("No SPI driver registered for scheme: " + scheme);
    }
}
