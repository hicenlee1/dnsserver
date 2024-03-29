package com.meizu.bigdata;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.*;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static com.meizu.bigdata.Hosts.HOST_IP_MAPPINGS;
import static com.meizu.bigdata.Hosts.byteIp2String;

@Slf4j
public class DnsServerHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {
//    private ProxyUdp proxyUdp;
//    public DnsServerHandler(ProxyUdp proxyUdp) {
//        this.proxyUdp = proxyUdp;
//    }

    private ProxyDnsResolver dnsResolver;
    public DnsServerHandler(ProxyDnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery msg) {
        try {
            int id = msg.id();
            DefaultDnsQuestion question = msg.recordAt(DnsSection.QUESTION);
            String name = question.name();
            Channel channel = ctx.channel();
            // udp 无法从channel中获取远端
            //SocketAddress socketAddress = channel.remoteAddress();
            InetSocketAddress sender = msg.sender();
            log.info("客户端{}: 请求域名{}对应的IP", sender, name);
            //log.info("messageid:{}", id);
            //log.info("DnsServerHandler read channel: {}", channel);

            if (HOST_IP_MAPPINGS.containsKey(name)) {
                //generate dns response
                DatagramDnsResponse dnsResponse = new DatagramDnsResponse(msg.recipient(), sender, id);
                dnsResponse.addRecord(DnsSection.QUESTION, question);
                DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(question.name(),
                        DnsRecordType.A,
                        DnsConst.DNS_TTL,
                        Unpooled.wrappedBuffer(HOST_IP_MAPPINGS.get(name)));
                dnsResponse.addRecord(DnsSection.ANSWER, queryAnswer);
                log.info("域名{}对应的IP为 {}",  name, byteIp2String(HOST_IP_MAPPINGS.get(name)));
                channel.writeAndFlush(dnsResponse);

                ReferenceCountUtil.release(msg);
                return;
            }

            //channel.attr(AttributeKey.<DatagramDnsQuery>valueOf(String.valueOf(id))).set(msg);
            //proxyUdp.send(name, id, channel);

            dnsResolver.send(msg, channel);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
        log.error(e.getMessage(), e);
    }
}

