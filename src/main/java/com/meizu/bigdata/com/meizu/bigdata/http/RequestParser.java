package com.meizu.bigdata.com.meizu.bigdata.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求参数解析器, 支持GET, POST * Created by whf on 12/23/15.
 */
@Slf4j
public class RequestParser {
    private FullHttpRequest fullReq;

    /**
     * 构造一个解析器 * @param req
     */
    public RequestParser(FullHttpRequest req) {
        this.fullReq = req;
    }

    /**
     * 解析请求参数 * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空Map * * @throws BaseCheckedException * @throws IOException
     */
    public Map<String, String> parse()  {
        HttpMethod method = fullReq.method();
        Map<String, String> parmMap = new HashMap<>();
        if (HttpMethod.GET == method) {
            // 是GET请求
            QueryStringDecoder decoder = new QueryStringDecoder(fullReq.uri());
            decoder.parameters().entrySet().forEach(entry -> {
                // entry.getValue()是一个List, 只取第一个元素
                parmMap.put(entry.getKey(), entry.getValue().get(0));
            });
        } else if (HttpMethod.POST == method) {
            // 是POST请求
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(fullReq);
            decoder.offer(fullReq);
            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData parm : parmList) {
                Attribute data = (Attribute) parm;
                try {
                    parmMap.put(data.getName(), data.getValue());
                } catch (IOException e) {
                    log.error("parse error", e);
                }
            }
        } else { // 不支持其它方法
            throw new IllegalArgumentException("只支持GET/POST解析");
            // 这是个自定义的异常, 可删掉这一行
        }
        return parmMap;
    }
}