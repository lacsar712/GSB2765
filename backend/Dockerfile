# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. 拷贝配置和 pom.xml
COPY settings.xml .
COPY pom.xml .

# 2. 预下载依赖 (只要 pom.xml 不变，这一层就会被 Docker 缓存)
RUN mvn -s settings.xml dependency:go-offline -B

# 3. 拷贝源码并打包
COPY src ./src
RUN mvn -s settings.xml package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
