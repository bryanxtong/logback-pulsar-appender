package com.github.bryan.logback.pulsar.delivery;

import com.github.bryan.logback.pulsar.ProducerRecord;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.TypedMessageBuilder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @since 0.0.1
 */
public class AsynchronousDeliveryStrategy implements DeliveryStrategy {

    @Override
    public <K, V, E> boolean send(Producer<V> producer, ProducerRecord<K, V> record, final E event,
                                  final FailedDeliveryCallback<E> failedDeliveryCallback) {
        try {
            TypedMessageBuilder<V> mb = producer.newMessage();
            K key = record.getKey();
            V value = record.getValue();
            Map<String, String> properties = record.getProperties();

            if (key instanceof byte[]) {
                mb.key(new String((byte[]) key));
            } else if (key instanceof String) {
                mb.key((String) key);
            }
            CompletableFuture<MessageId> resultFuture = mb.properties(properties).value(value).sendAsync();
            resultFuture.whenComplete((messageId, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                    failedDeliveryCallback.onFailedDelivery(event, exception);
                }
            });
            return true;
        } catch (Exception e) {
            failedDeliveryCallback.onFailedDelivery(event, e);
            return false;
        }
    }

}
