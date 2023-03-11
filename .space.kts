job("Build") {
    container("openjdk:17") {
        shellScript {
            content = """
              ./gradlew build  
          """
        }
    }
}

job("Test") {
    container("openjdk:17") {
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

    container("openjdk:17") {
        env["PUBLISHER_ID"] = Secrets("publisher_id")
        env["PUBLISHER_KEY"] = Secrets("publisher_key")

        shellScript {
            content = """
              ./gradlew publish    
          """
        }
    }
}
