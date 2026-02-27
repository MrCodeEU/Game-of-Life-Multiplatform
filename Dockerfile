FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon

FROM nginx:alpine
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
EXPOSE 80
