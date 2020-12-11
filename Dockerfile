FROM alpine:3.12.1 as java-runtime
RUN apk add openjdk11
RUN /usr/lib/jvm/java-11-openjdk/bin/jlink \
     --module-path /usr/lib/jvm/java-11-openjdk/jmods \
     --compress=2 \
     --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,java.xml,jdk.httpserver,jdk.unsupported \
     --no-header-files \
     --no-man-pages \
     --output /opt/jre-11

FROM alpine:3.12.1
MAINTAINER CYOSP <cyosp@cyosp.com>

ENV JAVA_HOME=/opt/jre-11
ENV PATH="$PATH:$JAVA_HOME/bin"

COPY --from=java-runtime /opt/jre-11 /opt/jre-11

ADD build/libs/ids-*.jar /ids.jar
CMD java -jar /ids.jar
