rootProject.name = "sk_prj1"

include("publisher-cli")
include("repository-api")
include("repository-core")
include("resolver-cli")
include("storage-local")

// Explicitly bind project paths to physical folder directories
project(":repository-api").projectDir = file("repository-api")
project(":repository-core").projectDir = file("repository-core")
project(":storage-local").projectDir = file("storage-local")
project(":publisher-cli").projectDir = file("publisher-cli")
project(":resolver-cli").projectDir = file("resolver-cli")
include("storage-network")