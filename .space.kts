job("Build") {
    container("openjdk:11") {
        shellScript {
            content = """
              ./gradlew build  
          """
        }
    }
}

job("Test") {
    container("openjdk:11") {
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

    container("openjdk:11") {
        env["PUBLISHER_ID"] = Secrets("publisher_id")
        env["PUBLISHER_KEY"] = Secrets("publisher_key")

        shellScript {
            content = """
              ./gradlew publish    
          """
        }
    }
}
