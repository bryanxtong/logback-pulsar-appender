# logback-pulsar-appender

The repos changed based on the code on https://github.com/danielwegener/logback-kafka-appender

when using this library, disable the pulsar logs as common logs and pulsar logs may lead to 
deadlock when they append logs during the process of creating the pulsar producer 
and origin kafka appender also has this problem(Enable kafka client logs to INFO will see it).

logging.level.org.apache.pulsar.client.impl=OFF

```
<configuration>

   <appender name="pulsarAppender" class="com.github.bryan.logback.pulsar.PulsarAppender">
       <encoder>
           <pattern>%msg</pattern>
       </encoder>
        <topic>logs</topic>
        <keyingStrategy class="com.github.bryan.logback.pulsar.keying.HostNameKeyingStrategy" />
        <deliveryStrategy class="com.github.bryan.logback.pulsar.delivery.AsynchronousDeliveryStrategy" />
        <brokerUrl>pulsar://localhost:6650</brokerUrl>
    </appender>
    <logger name="LogbackIntegrationIT" additivity="false" level="info">
        <appender-ref ref="pulsarAppender"/>
    </logger>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="warn">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```