FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon

FROM nginx:alpine
# pick up Alpine security fixes the nginx base image has not shipped yet
RUN apk upgrade --no-cache
COPY --from=builder /app/composeApp/build/dist/wasmJs/productionExecutable /usr/share/nginx/html
EXPOSE 80
