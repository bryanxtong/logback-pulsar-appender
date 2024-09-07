# logback-pulsar-appender

The repos changed based on the code on https://github.com/danielwegener/logback-kafka-appender

when using this library, disable the pulsar logs as common logs and pulsar logs may lead to 
deadlock when they append logs during the process of creating the pulsar producer 
and origin kafka appender also has this problem(Enable kafka client logs to INFO will see it).

logging.level.org.apache.pulsar.client.impl=OFF

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
   <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- This is the pulsarAppender -->
    <appender name="pulsarAppender" class="com.github.bryan.logback.pulsar.PulsarAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <topic>logs</topic>
        <keyingStrategy class="com.github.bryan.logback.pulsar.keying.NoKeyKeyingStrategy" />
        <deliveryStrategy class="com.github.bryan.logback.pulsar.delivery.AsynchronousDeliveryStrategy" />
        <!-- Optional parameter to use a fixed partition -->
        <!--
        <partition>0</partition>
        -->
        <!-- brokerUrl is the only mandatory -->
        <brokerUrl>pulsar://localhost:6650</brokerUrl>
        <!-- this is the fallback appender if pulsar is not available. -->
        <appender-ref ref="CONSOLE" />
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="pulsarAppender" />
    </root>
</configuration>
```