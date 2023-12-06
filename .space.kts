val image = "registry.jetbrains.team/p/ki/containers-ci/ci-corretto-17-firefox:1.0.1"

job("Build") {
    container(image) {
        shellScript {
            content = """
              ./gradlew build  
          """
        }
    }
}

job("Test") {
    container(image) {
        shellScript {
            content = """
              ./gradlew test  
          """
        }
    }
}

job("Release") {
    startOn {
        gitPush {
            enabled = false
        }
    }

    container(image) {
        env["PUBLISHER_ID"] = Secrets("publisher_id")
        env["PUBLISHER_KEY"] = Secrets("publisher_key")

        shellScript {
            content = """
              ./gradlew publish    
          """
        }
    }
}
