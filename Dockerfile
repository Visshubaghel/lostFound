FROM eclipse-temurin:11-jdk-alpine

WORKDIR /app

# Download dependencies dynamically during the build to avoid pushing binary files
RUN mkdir -p lib && \
    wget -q https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-sync/4.11.1/mongodb-driver-sync-4.11.1.jar -O lib/mongodb-driver-sync-4.11.1.jar && \
    wget -q https://repo1.maven.org/maven2/org/mongodb/mongodb-driver-core/4.11.1/mongodb-driver-core-4.11.1.jar -O lib/mongodb-driver-core-4.11.1.jar && \
    wget -q https://repo1.maven.org/maven2/org/mongodb/bson/4.11.1/bson-4.11.1.jar -O lib/bson-4.11.1.jar && \
    wget -q https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar -O lib/json-20240303.jar && \
    wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar -O lib/slf4j-api-1.7.36.jar && \
    wget -q https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar -O lib/slf4j-simple-1.7.36.jar

# Copy source code and static files
COPY src /app/src
COPY public /app/public

# Compile the application
RUN mkdir -p bin && \
    javac -cp "lib/*" src/api/AppServer.java src/db/DatabaseManager.java src/exceptions/*.java src/models/*.java src/service/*.java src/utils/*.java -d bin

# Expose port for Hugging Face
EXPOSE 7860

# Run the application
CMD ["java", "-cp", "bin:lib/*", "api.AppServer"]
