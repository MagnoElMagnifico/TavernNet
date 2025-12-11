FROM gradle:8.14.3-jdk21-alpine as build
WORKDIR /workspace

COPY . .
RUN gradle bootJar -x test

#########################

FROM amazoncorretto:21.0.9-alpine3.20
WORKDIR /tavernnet
COPY --from=build /workspace/build/libs/*SNAPSHOT.jar tavernnet.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "tavernnet.jar"]

