package com.github.bryan.logback.pulsar;

import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.AppenderAttachable;
import com.github.bryan.logback.pulsar.delivery.AsynchronousDeliveryStrategy;
import com.github.bryan.logback.pulsar.delivery.DeliveryStrategy;
import com.github.bryan.logback.pulsar.keying.KeyingStrategy;
import com.github.bryan.logback.pulsar.keying.NoKeyKeyingStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 0.0.1
 */
public abstract class PulsarAppenderConfig<E> extends UnsynchronizedAppenderBase<E> implements AppenderAttachable<E> {

    protected String brokerUrl = null ;
    protected String topic = null;

    protected Encoder<E> encoder = null;
    protected KeyingStrategy<? super E> keyingStrategy = null;
    protected DeliveryStrategy deliveryStrategy;

    protected Integer partition = null;

    protected boolean appendTimestamp = true;

    //pulsar message properties
    protected Map<String,String> messageProperties = new HashMap<>();

    protected boolean checkPrerequisites() {
        boolean errorFree = true;

        if (brokerUrl == null) {
            addError("No brokerUrl set for the appender named [\""
                    + name + "\"].");
            errorFree = false;
        }

        if (topic == null) {
            addError("No topic set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }

        if (encoder == null) {
            addError("No encoder set for the appender named [\"" + name + "\"].");
            errorFree = false;
        }

        if (keyingStrategy == null) {
            addInfo("No explicit keyingStrategy set for the appender named [\"" + name + "\"]. Using default NoKeyKeyingStrategy.");
            keyingStrategy = new NoKeyKeyingStrategy();
        }

        if (deliveryStrategy == null) {
            addInfo("No explicit deliveryStrategy set for the appender named [\""+name+"\"]. Using default asynchronous strategy.");
            deliveryStrategy = new AsynchronousDeliveryStrategy();
        }

        return errorFree;
    }

    public void setEncoder(Encoder<E> encoder) {
        this.encoder = encoder;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public void setKeyingStrategy(KeyingStrategy<? super E> keyingStrategy) {
        this.keyingStrategy = keyingStrategy;
    }

    public void addMessageProperty(String keyValue) {
        String[] split = keyValue.split("=", 2);
        if(split.length == 2){
            addMessagePropertyValue(split[0], split[1]);
        }
    }

    public void addMessagePropertyValue(String key, String value) {
        this.messageProperties.put(key,value);
    }

    public Map<String, String> getMessageProperties() {
        return messageProperties;
    }

    public void setDeliveryStrategy(DeliveryStrategy deliveryStrategy) {
        this.deliveryStrategy = deliveryStrategy;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public boolean isAppendTimestamp() {
        return appendTimestamp;
    }

    public void setAppendTimestamp(boolean appendTimestamp) {
        this.appendTimestamp = appendTimestamp;
    }

}
