val artifactoryBaseUrl = sys.env("ARTIFACTORY_URL")
val artifactoryHost = sys.env("ARTIFACTORY_HOST")
val artifactoryUsername = sys.env("ARTIFACTORY_USERNAME")
val artifactoryPassword = sys.env("ARTIFACTORY_PASSWORD")

externalResolvers := {
    ("Brewlabs Snapshot Maven Repository" at artifactoryBaseUrl + "/artifactory/libs-snapshot/") +:
    ("Brewlabs Release Maven Repository" at artifactoryBaseUrl + "/artifactory/libs-release/") +:
    externalResolvers.value
}

credentials += Credentials("Artifactory Realm", artifactoryHost, artifactoryUsername, artifactoryPassword)

