package com.github.bryan.logback.pulsar;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import com.github.bryan.logback.pulsar.delivery.FailedDeliveryCallback;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.ProducerImpl;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @since 0.0.1
 */
public class PulsarAppender<E> extends PulsarAppenderConfig<E> {

    /**
     * Kafka clients uses this prefix for its slf4j logging.
     * This appender defers appends of any Kafka logs since it could cause harmful infinite recursion/self feeding effects.
     */
    //private static final String KAFKA_LOGGER_PREFIX = KafkaProducer.class.getPackage().getName().replaceFirst("\\.producer$", "");
    private static final String PULSAR_LOGGER_PREFIX = ProducerImpl.class.getPackage().getName().replaceFirst("\\.client.impl$", "");

    private LazyProducer lazyProducer = null;

    private volatile PulsarClient pulsarClient = null;
    private final AppenderAttachableImpl<E> aai = new AppenderAttachableImpl<E>();
    private final ConcurrentLinkedQueue<E> queue = new ConcurrentLinkedQueue<E>();
    private final FailedDeliveryCallback<E> failedDeliveryCallback = new FailedDeliveryCallback<E>() {
        @Override
        public void onFailedDelivery(E evt, Throwable throwable) {
            aai.appendLoopOnAppenders(evt);
        }
    };

    @Override
    public void doAppend(E e) {
        ensureDeferredAppends();
        if (e instanceof ILoggingEvent && ((ILoggingEvent)e).getLoggerName().startsWith(PULSAR_LOGGER_PREFIX)) {
            deferAppend(e);
        } else {
            super.doAppend(e);
        }
    }

    @Override
    public void start() {
        // only error free appenders should be activated
        if (!checkPrerequisites()) return;

        if (partition != null && partition < 0) {
            partition = null;
        }

        lazyProducer = new LazyProducer();

        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (lazyProducer != null && lazyProducer.isInitialized()) {
            try {
                Producer<byte[]> producer = lazyProducer.get();
                if(null != producer && producer.isConnected()){
                    producer.close();
                }
            } catch (PulsarClientException e) {
                this.addWarn("Failed to shut down pulsar producer: " + e.getMessage(), e);
            }
            lazyProducer = null;
        }

        if(pulsarClient != null && !pulsarClient.isClosed()){
            try {
                pulsarClient.close();
            } catch (PulsarClientException e) {
                this.addWarn("Failed to shut down pulsar client: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void addAppender(Appender<E> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<E>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<E> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<E> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<E> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }

    @Override
    protected void append(E e) {
        final byte[] payload = encoder.encode(e);
        final byte[] key = keyingStrategy.createKey(e);

        final Long timestamp = isAppendTimestamp() ? getTimestamp(e) : null;

        //add producer properties
        if(e instanceof ILoggingEvent){
            this.getMessageProperties().put("level",((ILoggingEvent)e).getLevel().toString());
        }
        Context context = encoder.getContext();
        if(null != context){
            Map<String, String> copyOfPropertyMap = context.getCopyOfPropertyMap();
            copyOfPropertyMap.entrySet().forEach(c-> this.getMessageProperties().put(c.getKey(), c.getValue()));
        }
        //add producer properties

        final ProducerRecord<byte[], byte[]> record = new ProducerRecord<>(key, payload, messageProperties);

        //both common logs and pulsar client logs append may lead to dead lock when creating the pulsar producer,
        //disable pulsar client logs as a temp solution [logging.level.org.apache.pulsar.client.impl=ERROR]
        final Producer<byte[]> producer = lazyProducer.get();
        if (producer != null) {
            deliveryStrategy.send(lazyProducer.get(), record, e, failedDeliveryCallback);
        } else {
            failedDeliveryCallback.onFailedDelivery(e, null);
        }
    }

    protected Long getTimestamp(E e) {
        if (e instanceof ILoggingEvent) {
            return ((ILoggingEvent) e).getTimeStamp();
        } else {
            return System.currentTimeMillis();
        }
    }

    /**
     * If partition is not null , use that particular partition
     * During the appending process, there should be no other logs which appends for pulsar client code(pulsar client logging) which would lead to dead lock
     * logging.level.org.apache.pulsar.client.impl=OFF to avoid logging for pulsar clients
     * @return Producer
     * @throws PulsarClientException exception when creating the pulsar producer
     */
    protected Producer<byte[]> createProducer() throws PulsarClientException {
        if(null == pulsarClient){
            pulsarClient = PulsarClient.builder()
                    .serviceUrl(brokerUrl)
                    .build();
        }
        ProducerBuilder<byte[]> producerBuilder = pulsarClient
                .newProducer()
                .topic(topic);

        //if partition is set, use that partition otherwise use default roundrobin
        if(partition!= null){
            producerBuilder.messageRouter(new MessageRouter() {
                @Override
                public int choosePartition(Message<?> msg, TopicMetadata metadata) {
                    return partition;
                }
            });
        }
        Producer<byte[]> producer = producerBuilder.create();
        return producer;
    }

    private void deferAppend(E event) {
        queue.add(event);
    }

    // drains queue events to super
    private void ensureDeferredAppends() {
        E event;

        while ((event = queue.poll()) != null) {
            super.doAppend(event);
        }
    }

    /**
     * Lazy initializer for producer, patterned after commons-lang.
     *
     * @see <a href="https://commons.apache.org/proper/commons-lang/javadocs/api-3.4/org/apache/commons/lang3/concurrent/LazyInitializer.html">LazyInitializer</a>
     */
    protected class LazyProducer {

        private volatile Producer<byte[]> producer;

        public Producer<byte[]> get() {
            Producer<byte[]> result = this.producer;
            if (result == null) {
                synchronized(this) {
                    result = this.producer;
                    if(result == null) {
                        this.producer = result = this.initialize();
                    }
                }
            }
            return result;
        }

        protected Producer<byte[]> initialize() {

            Producer<byte[]> producer = null;
            try {
                producer = createProducer();
            } catch (Exception e) {
                e.printStackTrace();
                addError("error creating producer", e);
            }
            return producer;
        }

        public boolean isInitialized() { return producer != null; }
    }

    public PulsarClient getPulsarClient() {
        return pulsarClient;
    }

}
