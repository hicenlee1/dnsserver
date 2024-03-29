package com.meizu.bigdata;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.meizu.bigdata.Hosts.HOST_IP_MAPPINGS;

@Slf4j
public class ProxyDnsResolver {

    String[] defaultDnsServer = new String[] {"172.16.80.104", "172.16.80.105"};
    int defaultDnsPort = 53;

    EventLoopGroup group;
    DnsNameResolver resolver;

    public void setDefaultDnsServer(String dnsServer[]) {
        this.defaultDnsServer = dnsServer;
    }

    public void setDefaultDnsPort(int dnsPort) {
        this.defaultDnsPort = dnsPort;
    }

    public void init() {
        String dnsServer = System.getProperty("DNS.SERVER");
        String dnsPort = System.getProperty("DNS.PORT");
        if (dnsServer != null) {
            setDefaultDnsServer(dnsServer.split(","));
        }
        if (dnsPort != null) {
            setDefaultDnsPort(Integer.parseInt(dnsPort));
        }

        List<InetSocketAddress> dnsServerList = Arrays.stream(defaultDnsServer)
                .map(server -> new InetSocketAddress(server, defaultDnsPort))
                .collect(Collectors.toList());

        group = new NioEventLoopGroup();
        resolver = new DnsNameResolverBuilder(group.next())
                .channelType(NioDatagramChannel.class)
                .nameServerProvider(new SequentialDnsServerAddressStreamProvider(dnsServerList))
                .queryTimeoutMillis(3000)
                //.maxQueriesPerResolve(20)
                .build();
    }

    public void stop() {
        if (resolver != null) {
            resolver.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    public Future<InetAddress> resolve(String domain) {
        return resolver.resolve(domain);
    }


    //public void send2(String domain, DefaultDnsQuestion question, int msgID, InetSocketAddress sender, InetSocketAddress recipient, Channel localChannel) {
//    public void send2(DatagramDnsQuery msg, DefaultDnsQuestion question, int msgID, InetSocketAddress sender, InetSocketAddress recipient, Channel localChannel) {
    public void send(DatagramDnsQuery msg, Channel localChannel) {
        DefaultDnsQuestion question = msg.recordAt(DnsSection.QUESTION);
        int msgId = msg.id();
        InetSocketAddress sender = msg.sender();
        InetSocketAddress recipient = msg.recipient();
        String domain = question.name();
        resolver.resolve(domain).addListener(
                (Future<InetAddress> future) -> {
                    if (future.isSuccess()) {
                        InetAddress addresses = future.getNow();
                        //generate dns response
                        DatagramDnsResponse dnsResponse = new DatagramDnsResponse(recipient, sender, msgId);
                        dnsResponse.addRecord(DnsSection.QUESTION, question);
                        DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(domain,
                                DnsRecordType.A,
                                DnsConst.DNS_TTL,
                                Unpooled.wrappedBuffer(addresses.getAddress()));
                        dnsResponse.addRecord(DnsSection.ANSWER, queryAnswer);
                        localChannel.writeAndFlush(dnsResponse);
                    } else {
                        Throwable cause = future.cause();
                        log.error("解析[" + domain + "]失败", cause);
                    }
                    //这里为何不用释放？ 释放会报错
                    //ReferenceCountUtil.release(msg);
                }
        );
    }

}
