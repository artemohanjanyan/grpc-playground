ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.6"

lazy val root = (project in file("."))
  .settings(
    name := "grpc-playground"
  )

// https://mvnrepository.com/artifact/io.grpc/grpc-stub
libraryDependencies += "io.grpc" % "grpc-stub" % "1.75.0"
// https://mvnrepository.com/artifact/io.grpc/grpc-netty
libraryDependencies += "io.grpc" % "grpc-netty" % "1.75.0"
// https://mvnrepository.com/artifact/io.grpc/grpc-services
libraryDependencies += "io.grpc" % "grpc-services" % "1.75.0"
// https://mvnrepository.com/artifact/io.grpc/grpc-services
libraryDependencies += "io.grpc" % "grpc-services" % "1.75.0" % "runtime"

// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java
libraryDependencies += "com.google.protobuf" % "protobuf-java" % "4.32.1"
// https://mvnrepository.com/artifact/com.google.protobuf/protobuf-java-util
libraryDependencies += "com.google.protobuf" % "protobuf-java-util" % "4.32.1"