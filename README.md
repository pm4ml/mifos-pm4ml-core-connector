# mojaloop-finflux-client-adapter
Sample project for a Mojaloop adapter for a finflux core banking system

to generate the Java Rest DSL router and Model files (In parent pom): mvn clean install

To Build the Project: mvn clean package

To Build the project using Docker: docker build -t client-adapter .

To run the project using Docker: docker run -p 3000:3000 -p 8080:8080 -t client-adapter

To run the Integration Tests (run mvn clean install under client-adapter folder first): mvn -Pdocker-it clean install

The camel-jasypt-2.11.1.jar is to encryp/decrypt secure property file variables (algorithm can also be specified with -a)
java -jar camel-jasypt-2.5.0.jar -c encrypt -p secret -i myword

Architecture diagram:
 