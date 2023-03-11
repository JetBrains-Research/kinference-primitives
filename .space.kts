job("Build") {
    container("amazoncorretto:17") {
        shellScript {
            content = """
              ./gradlew build  
          """
        }
    }
}

job("Test") {
    container("amazoncorretto:17") {
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

    container("amazoncorretto:17") {
        env["PUBLISHER_ID"] = Secrets("publisher_id")
        env["PUBLISHER_KEY"] = Secrets("publisher_key")

        shellScript {
            content = """
              ./gradlew publish    
          """
        }
    }
}
