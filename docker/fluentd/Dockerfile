FROM fluent/fluentd:latest

USER root

RUN apk update

RUN apk --update add tzdata && \
    cp /usr/share/zoneinfo/Asia/Tokyo /etc/localtime && \
    apk del tzdata && \
    rm -rf /var/cache/apk/*

RUN fluent-gem install fluent-plugin-elasticsearch -v 1.10.2

ARG lang
ENV LANG ${lang}
ENV LANGUAGE ja_JP:ja
ENV LC_ALL ${lang}

RUN mkdir -p /fluentd/buffer

COPY fluent.conf /fluentd/etc
COPY fluent-es.conf /fluentd/etc

EXPOSE 24224
