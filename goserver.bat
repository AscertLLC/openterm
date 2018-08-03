set WD=%~dp0
rem - Would like to keep this so it can run in headless mode if possible
rem gradlew -Djava.util.logging.config.file=%WD%/jul.properties -Dcom.ascert.open.term.server.port=5900 execute
gradlew -Djava.awt.headless=true -Djava.util.logging.config.file=%WD%/jul.properties -Dcom.ascert.open.term.server.websocket=5800 -Dcom.ascert.open.term.server.socket=5900 execute