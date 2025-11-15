# Minimal Dockerfile for the Java socket server
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . /app
RUN mkdir -p data
RUN if [ ! -f data/counter.txt ]; then printf "00000\n" > data/counter.txt; fi
RUN javac -source 17 -target 17 src/*.java
EXPOSE 8080
ENV PORT 8080
CMD ["sh", "-c", "java -cp src MainServer"]
