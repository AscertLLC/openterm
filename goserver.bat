set WD=%~dp0
gradlew -Djava.util.logging.config.file=%WD%/jul.properties -Dcom.ascert.open.term.server.port=5900 execute