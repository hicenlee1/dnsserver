package com.meizu.bigdata;


import com.meizu.bigdata.com.meizu.bigdata.http.HttpServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.DatagramDnsQueryDecoder;
import io.netty.handler.codec.dns.DatagramDnsResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DNSServer {
    /**
     * usage: <br/>
     * java -jar ***.jar
     *
     * default proxy dns server ip: 172.16.80.104<br/>
     * default proxy dns server port: 53<br/>
     *
     * customize proxy dns server and port
     * java -jar ***.jar -DDNS.SERVER=172.16.80.104,172.16.80.105 -DDNS.PORT=53
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
//        ProxyUdp proxyUdp = new ProxyUdp();
//        proxyUdp.init();

        ProxyDnsResolver resolver = new ProxyDnsResolver();
        resolver.init();

        final int[] num = {0};
        final NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) {
                        //nioDatagramChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                        nioDatagramChannel.pipeline().addLast(new DatagramDnsQueryDecoder());
                        //nioDatagramChannel.pipeline().addLast(new DnsServerHandler(proxyUdp));
                        nioDatagramChannel.pipeline().addLast(new DnsServerHandler(resolver));
                        nioDatagramChannel.pipeline().addLast(new DatagramDnsResponseEncoder());

                    }
                }).option(ChannelOption.SO_BROADCAST, true);

        int port = 53;
        ChannelFuture future = bootstrap.bind(port).addListener(future1 -> {
            log.info("server listening port:{}", port);
        });

        future.channel().closeFuture().addListener(future1 -> {
            if (future.isSuccess()) {
                log.info(future.channel().toString());
            }
        });

        new HttpServer(8900, resolver).start();
    }

}
