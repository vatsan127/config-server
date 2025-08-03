FROM eclipse-temurin:17-jre

# Project structure inside container
# dependencies - /app/lib
# maven - /app/META-INF
# source - /app

ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app

ENTRYPOINT ["java","-cp","app:app/lib/*","dev.srivatsan.kubeapi.KubeapiApplication"]