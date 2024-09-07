package com.github.bryan.logback.pulsar.delivery;

import com.github.bryan.logback.pulsar.ProducerRecord;
import org.apache.pulsar.client.api.Producer;

/**
 * Interface for DeliveryStrategies.
 * @since 0.0.1
 */
public interface DeliveryStrategy {

    /**
     * Sends a message to a pulsar producer and somehow deals with failures.
     *
     * @param producer the backing pulsar producer
     * @param event the originating logging event
     * @param failedDeliveryCallback a callback that handles messages that could not be delivered with best-effort.
     * @param record the key/value of the message, just an object to get key/value, not the sending type to pulsar, may enhance to [key,value, properties]
     * @param <K> the key type of a persisted log message.
     * @param <V> the value type of a persisted log message.
     * @param <E> the type of the logging event.
     * @return {@code true} if the message could be sent successfully, {@code false} otherwise.
     */
    <K,V,E> boolean send(Producer<V> producer, ProducerRecord<K,V> record, E event, FailedDeliveryCallback<E> failedDeliveryCallback);

}
