package com.meizu.bigdata.com.meizu.bigdata.http;

import com.meizu.bigdata.Hosts;
import com.meizu.bigdata.ProxyDnsResolver;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.netty.handler.codec.http.HttpUtil.is100ContinueExpected;

@Slf4j
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    ProxyDnsResolver resolver;
    public HttpRequestHandler(ProxyDnsResolver resolver) {
        this.resolver = resolver;
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        //100 Continue
        if (is100ContinueExpected(req)) {
            ctx.write(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.CONTINUE));
        }
        // 获取请求的uri
        String uri = req.uri();
        //Map<String, String> parse = new RequestParser(req).parse();
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        List<String> queryHosts = decoder.parameters().get("host");

        String msg = "<html><head><title>DNS 查询</title></head><body>DNS查询，请访问 <a href='/?host=sct.meizu.com'>查看IP</a></body></html>";
        if (queryHosts != null) {
            StringBuilder sb = new StringBuilder(100);
            for (String host : queryHosts) {
                if (!StringUtil.isNullOrEmpty(host)) {
                    String localHost = host.trim();
                    if (!StringUtil.endsWith(localHost, '.')) {
                        localHost = localHost + ".";
                    }
                    byte[] ipBytes = Hosts.HOST_IP_MAPPINGS.get(localHost);
                    if (ipBytes == null) {
                        //不推荐阻塞在IO 线程
                        //ipBytes = resolver.resolve(host).get().getAddress();

                        InetAddress inetAddress = resolver.resolve(host).get(2000, TimeUnit.SECONDS);
                        if (inetAddress != null) {
                            ipBytes = inetAddress.getAddress();
                        }

                    }
                    String ipStr = Hosts.byteIp2String(ipBytes);
                    sb.append(host).append("对应的IP:   ").append(ipStr).append("<br/>");
                }
            }
            msg = "<html><head><title>DNS查询</title></head><body>"+ sb.toString() + "</body></html>";
        }

//        String msg = "<html><head><title>DNS查询</title></head><body>你请求uri为：" + uri
//                + sb.toString()
//                + "</body></html>";
        // 创建http响应
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        //response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        // 将html write到客户端
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(cause.getLocalizedMessage(), CharsetUtil.UTF_8));
        // 设置头信息
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

        super.exceptionCaught(ctx, cause);
    }
}