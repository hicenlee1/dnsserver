package com.meizu.bigdata.com.meizu.bigdata.http;

import com.meizu.bigdata.ProxyDnsResolver;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class HttpServer {
    int port ;
    ProxyDnsResolver resolver;

    public HttpServer(int port){
        this.port = port;
    }


    public HttpServer(int port, ProxyDnsResolver resolver){
        this.port = port;
        this.resolver = resolver;
    }

    public void start() throws Exception{
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup();
        EventLoopGroup work = new NioEventLoopGroup();

        try {
            bootstrap.group(boss,work)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerInitializer(resolver));

            //ChannelFuture f = bootstrap.bind(new InetSocketAddress(port)).sync();
            ChannelFuture f = bootstrap.bind(new InetSocketAddress(port)).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("HTTP服务在端口[" + port + "]绑定成功");
                } else {
                    log.error("HTTP服务在端口[" + port + "]绑定失败");
                }
            });
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }

    }


    public static void main(String[] args) throws Exception {
        new HttpServer(8888).start();
    }

}

