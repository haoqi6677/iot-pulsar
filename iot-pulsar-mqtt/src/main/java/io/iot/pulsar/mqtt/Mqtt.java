package io.iot.pulsar.mqtt;

import static java.util.Objects.requireNonNull;
import io.iot.pulsar.agent.PulsarAgent;
import io.iot.pulsar.common.options.IotPulsarMqttOptions;
import io.iot.pulsar.mqtt.processor.ConnectProcessor;
import io.iot.pulsar.mqtt.processor.MqttProcessorController;
import io.netty.handler.codec.mqtt.MqttMessageType;
import javax.annotation.Nonnull;
import lombok.Getter;

public class Mqtt {
    @Getter
    private final PulsarAgent pulsarAgent;
    @Getter
    private final IotPulsarMqttOptions options;

    @Getter
    private final MqttProcessorController processorController = new MqttProcessorController();

    private Mqtt(@Nonnull PulsarAgent pulsarAgent, @Nonnull IotPulsarMqttOptions options) {
        this.options = options;
        this.pulsarAgent = pulsarAgent;
    }

    {
        // Automatic register
        processorController.register(MqttMessageType.CONNECT, new ConnectProcessor(this));
    }

    @Nonnull
    public static Mqtt create(@Nonnull PulsarAgent pulsarAgent, @Nonnull IotPulsarMqttOptions options) {
        requireNonNull(pulsarAgent, "Argument [pulsar agent] can not be null");
        requireNonNull(pulsarAgent, "Argument [options] can not be null");
        return new Mqtt(pulsarAgent, options);
    }

    @Nonnull
    public MqttInboundHandler createInboundHandler() {
        return new MqttInboundHandler(this);
    }

    public void close() {

    }
}
