package com.meizu.bigdata;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import java.net.InetSocketAddress;
import static com.meizu.bigdata.Hosts.byteIp2String;

@Slf4j
@Deprecated
public class ProxyUdp {
    private Channel localChannel;
    private Channel proxyChannel;
    String defaultDnsServer = "172.16.80.104";
    int defaultDnsPort = 53;

    public void init() throws InterruptedException {
        String dnsServer = System.getProperty("DNS.SERVER");
        String dnsPort = System.getProperty("DNS.PORT");
        if (dnsServer != null) {
            setDefaultDnsServer(dnsServer);
        }
        if (dnsPort != null) {
            setDefaultDnsPort(Integer.parseInt(dnsPort));
        }

        EventLoopGroup proxyGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(proxyGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new DatagramDnsQueryEncoder())
                                .addLast(new DatagramDnsResponseDecoder())
                                .addLast(new SimpleChannelInboundHandler<DatagramDnsResponse>() {
                                    @Override
                                    public void channelActive(ChannelHandlerContext ctx) {
                                        //log.info(ctx.channel().toString());
                                    }

                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, DatagramDnsResponse msg) {
                                        DatagramDnsQuery dnsQuery = localChannel.attr(AttributeKey.<DatagramDnsQuery>valueOf(String.valueOf(msg.id()))).get();
                                        log.info("proxy read local channel: {}", localChannel);
                                        DnsQuestion question = msg.recordAt(DnsSection.QUESTION);

                                        DatagramDnsResponse dnsResponse = new DatagramDnsResponse(dnsQuery.recipient(), dnsQuery.sender(), msg.id());
                                        dnsResponse.addRecord(DnsSection.QUESTION, question);

                                        //try1
//                                        dnsResponse.setAuthoritativeAnswer(msg.isAuthoritativeAnswer());
//                                        dnsResponse.setTruncated(msg.isTruncated());
//                                        dnsResponse.setRecursionAvailable(msg.isRecursionAvailable());
//                                        dnsResponse.setRecursionDesired(msg.isRecursionDesired());

//                                        DnsSection[] responseSection = new DnsSection[] {DnsSection.ANSWER, DnsSection.AUTHORITY, DnsSection.ADDITIONAL};
//                                        for (DnsSection dnsSection: responseSection) {
//
//                                            for (int i = 0, count = msg.count(dnsSection); i < count; i++) {
//                                                DnsRecord record = msg.recordAt(dnsSection, i);
//                                                dnsResponse.addRecord(dnsSection, record);
//
//
//
//                                                //
////                                                DnsRawRecord raw = (DnsRawRecord) record;
////                                                DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(
////                                                        raw.name(),
////                                                        raw.type(),
////                                                        DnsConst.DNS_TTL,
////                                                        Unpooled.wrappedBuffer(ByteBufUtil.getBytes(raw.content())));
////
////                                                dnsResponse.addRecord(dnsSection, queryAnswer);
//                                            }
//                                        }


//                                        // try2
                                        for (int i = 0, count = msg.count(DnsSection.ANSWER); i < count; i++) {
                                            DnsRecord record = msg.recordAt(DnsSection.ANSWER, i);
                                            if (record.type() == DnsRecordType.A) {
                                                // just print the IP after query
                                                DnsRawRecord raw = (DnsRawRecord) record;
                                                DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(
                                                        question.name(),
                                                        DnsRecordType.A,
                                                        DnsConst.DNS_TTL,
                                                        Unpooled.wrappedBuffer(ByteBufUtil.getBytes(raw.content())));
                                                dnsResponse.addRecord(DnsSection.ANSWER, queryAnswer);
                                                log.info("ProxyUdp查询域名{}对应的IP为 {}",  question.name(), byteIp2String(ByteBufUtil.getBytes(raw.content())));
                                            }
                                        }

                                        localChannel.writeAndFlush(dnsResponse);
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
                                        log.error(e.getMessage(), e);
                                    }
                                });

                    }
                });
        proxyChannel = b.bind(0).sync().addListener(future1 -> {
            log.info("绑定成功");
        }).channel();
    }

    public void send(String domain, int id, Channel localChannel) {
        this.localChannel = localChannel;
        DnsQuery query = new DatagramDnsQuery(null, new InetSocketAddress(this.defaultDnsServer, defaultDnsPort), id).setRecord(
                DnsSection.QUESTION,
                new DefaultDnsQuestion(domain, DnsRecordType.A));
        this.proxyChannel.writeAndFlush(query);
    }


    public void setDefaultDnsServer(String dnsServer) {
        this.defaultDnsServer = dnsServer;
    }

    public void setDefaultDnsPort(int dnsPort) {
        this.defaultDnsPort = dnsPort;
    }

}
