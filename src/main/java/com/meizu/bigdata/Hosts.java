package com.meizu.bigdata;

import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Hosts {
    public static Map<String, byte[]> HOST_IP_MAPPINGS = new ConcurrentHashMap<>();
    private static long hostFileLastModified = 0;

    static {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                initHostsMapping();
            }
        }, 5, 60, TimeUnit.SECONDS);

    }

    public static void initHostsMapping() {
        InputStream is = null;
        String jarPath = System.getProperty("user.dir");
        File hostFile = new File(jarPath, "hosts.txt");
        long localLastModified = 0;
        try {
            if (hostFile.exists()) {
                localLastModified = hostFile.lastModified();
                if (localLastModified > hostFileLastModified) {
                    is = new FileInputStream(hostFile);
                }
            } else {
                log.info("local host file does not exist, use build-in file");
                is = DnsServerHandler.class.getResourceAsStream("/hosts.txt");
            }
            if (is != null) {
                log.info("dns config changes, refresh");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
                String line = bufferedReader.readLine();
                while (line != null) {
                    String trimline = line.trim();
                    if (!trimline.startsWith("#") && !"".equals(trimline)){
                        String[] tokens = trimline.split("\\s+");
                        String ip = tokens[0];
                        for (int i = 1; i < tokens.length; i++) {
                            String host = tokens[i];
                            if (ip != null && host != null) {
                                // DNS 配置的Host 必须以 “.” 结尾，比如 "sct.meizu.com."
                                String trim = host.trim();
                                if (StringUtil.endsWith(trim, '.')) {
                                    HOST_IP_MAPPINGS.put(trim, ipToByteArray(ip));
                                } else {
                                    HOST_IP_MAPPINGS.put(trim + ".", ipToByteArray(ip));
                                }
                            }
                        }

                    }
                    line = bufferedReader.readLine();
                }
            } else {
                log.info("dns config does not change, skip refreshing");
            }
        } catch (Exception e) {
            log.error("刷新host-ip mapping error", e);
        } finally {
            if (localLastModified > hostFileLastModified) {
                hostFileLastModified = localLastModified;
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    log.error("is close error", ioe);
                }
            }
        }
    }
    
    private static byte[] ipToByteArray(String ip) {
        String[] split = ip.split("\\.");
        byte[] b = new byte[4];
        b[0] = Integer.valueOf(split[0]).byteValue();
        b[1] = Integer.valueOf(split[1]).byteValue();
        b[2] = Integer.valueOf(split[2]).byteValue();
        b[3] = Integer.valueOf(split[3]).byteValue();
        return b;
    }

    public static String byteIp2String(byte[] ipaddress) {
        if (ipaddress == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (ipaddress == null && ipaddress.length != 4) {
            return "";
        } else {
            int i = 0;
            for (byte b : ipaddress) {
                if (i != 0) {
                    sb.append(".");
                }
                sb.append(b & 0xFF);
                i++;
            }

        }
        return sb.toString();
    }


    public static void main(String[] args) {
        //System.out.println(Byte.parseByte("156"));
        System.out.println(Integer.valueOf(156).byteValue());

        byte byte_minus_100 = (byte) 0b10011100;
        System.out.println(byte_minus_100 & 0xFF);
    }


}

