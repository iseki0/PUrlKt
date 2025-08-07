rootProject.name = "purlkt"

toolchainManagement {
    jvm { 
        javaRepositories {
            repository("foojay") { 
                resolverClass = org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java
            }
        }
    }
}
