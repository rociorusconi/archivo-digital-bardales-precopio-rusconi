# ---------- Stage 1: build the .jar ----------
# Runs on the machine that builds (BUILDPLATFORM): the .jar works on any processor, so there is
# no need to emulate the server's one (e.g. building on an ARM Mac for an amd64 VPS).
FROM --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS build
WORKDIR /app

# The pom uses maven-toolchains-plugin, which needs a toolchains.xml pointing to a JDK 21.
# (GitHub Actions' setup-java creates one automatically; inside the image we create it here.)
RUN mkdir -p /root/.m2 && printf '%s\n' \
    '<toolchains><toolchain><type>jdk</type>' \
    '<provides><version>21</version></provides>' \
    "<configuration><jdkHome>${JAVA_HOME}</jdkHome></configuration>" \
    '</toolchain></toolchains>' > /root/.m2/toolchains.xml

# Dependencies first: this layer is cached while pom.xml does not change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

# Tests are not run here: the GitHub Actions workflow runs them before building the image.
COPY src src
RUN ./mvnw -B -q -DskipTests package

# ---------- Stage 2: run the .jar (JRE only, no Maven or sources) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
