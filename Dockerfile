FROM debian:trixie-slim AS java-runtime
RUN apt update && apt upgrade -y
RUN apt install -y openjdk-21-jdk-headless
RUN jlink \
     --module-path $(echo $(dirname $(readlink $(readlink $(which jlink))))/../jmods) \
     --compress=2 \
     --add-modules java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.management.rmi,java.naming,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,java.xml,jdk.httpserver,jdk.unsupported \
     --no-header-files \
     --no-man-pages \
     --output /opt/jre-21

FROM debian:trixie-slim
ARG TARGETPLATFORM
MAINTAINER CYOSP <cyosp@cyosp.com>

RUN apt update && apt upgrade -y

RUN apt -y install locales && sed -i '/en_US.UTF-8/s/^# //g' /etc/locale.gen && locale-gen
ENV LANG en_US.UTF-8

ENV JAVA_HOME=/opt/jre-21
ENV PATH="$PATH:$JAVA_HOME/bin"

COPY --from=java-runtime /opt/jre-21 /opt/jre-21

RUN apt install -y bc imagemagick ffmpeg
ADD docker-context/generateAlternativeFormats.sh /generateAlternativeFormats.sh
RUN chmod +x /generateAlternativeFormats.sh

ADD docker-context/$TARGETPLATFORM/ids-*.jar /ids.jar
CMD java -jar /ids.jar
