# logback-pulsar-appender

The repos changed based on the code on https://github.com/danielwegener/logback-kafka-appender

when using this library, disable the pulsar logs as common logs and pulsar logs may lead to 
dead lock and origin kafka appender also has this problem.

logging.level.org.apache.pulsar.client.impl=OFF