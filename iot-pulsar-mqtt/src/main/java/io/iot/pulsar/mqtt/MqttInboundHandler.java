package io.iot.pulsar.mqtt;

import com.google.common.base.Strings;
import io.iot.pulsar.mqtt.endpoint.MqttEndpoint;
import io.iot.pulsar.mqtt.endpoint.MqttEndpointImpl;
import io.iot.pulsar.mqtt.endpoint.MqttEndpointProperties;
import io.iot.pulsar.mqtt.endpoint.RejectOnlyMqttEndpoint;
import io.iot.pulsar.mqtt.messages.Identifier;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.UnsupportedMessageTypeException;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttVersion;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ThreadSafe
public class MqttInboundHandler extends ChannelInboundHandlerAdapter {
    private final Mqtt mqtt;
    private MqttEndpoint mqttEndpoint;
    private ChannelHandlerContext ctx;

    public MqttInboundHandler(@Nonnull Mqtt mqtt) {
        this.mqtt = mqtt;
    }

    @Override
    public void channelRegistered(@Nonnull ChannelHandlerContext ctx) throws Exception {
        MqttInboundHandler.this.ctx = ctx;
        super.channelRegistered(ctx);
    }

    @Override
    public void channelRead(@Nonnull ChannelHandlerContext ctx, @Nonnull Object msg) {
        if (!(msg instanceof MqttMessage)) {
            ctx.fireExceptionCaught(new UnsupportedMessageTypeException(msg, MqttMessage.class));
            return;
        }
        // --- Check codec
        final DecoderResult result = ((MqttMessage) msg).decoderResult();
        if (result.isFailure()) {
            MqttInboundHandler.this.mqttEndpoint = new RejectOnlyMqttEndpoint(ctx.channel());
            mqttEndpoint.swallow((MqttMessage) msg);
            return;
        }
        MqttFixedHeader fixed = ((MqttMessage) msg).fixedHeader();
        if (fixed.messageType() == MqttMessageType.CONNECT) {
            MqttConnectMessage connectMessage = (MqttConnectMessage) msg;
            if (this.mqttEndpoint != null) {
                mqttEndpoint.close();
                return;
            }
            final MqttConnectVariableHeader var = connectMessage.variableHeader();
            final MqttConnectPayload payload = connectMessage.payload();
            final boolean assignedIdentifier = Strings.isNullOrEmpty(payload.clientIdentifier());
            final String identifier;
            if (assignedIdentifier) {
                identifier = UUID.randomUUID().toString();
            } else {
                identifier = payload.clientIdentifier();
            }
            // preparing endpoint
            final MqttEndpointProperties properties = MqttEndpointProperties
                    .builder()
                    .cleanSession(var.isCleanSession())
                    .build();
            MqttInboundHandler.this.mqttEndpoint = MqttEndpointImpl.builder()
                    .identifier(Identifier.create(identifier, assignedIdentifier))
                    .ctx(ctx)
                    .version(MqttVersion.fromProtocolNameAndLevel(var.name(), (byte) var.version()))
                    .properties(properties)
                    .processorController(mqtt.getProcessorController())
                    .build();
        }
        mqttEndpoint.swallow((MqttMessage) msg);
    }
}
