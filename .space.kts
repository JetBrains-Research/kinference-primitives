val image = "amazoncorretto:17-alpine3.18"

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

job(image) {
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
