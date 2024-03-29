开发环境中经常需要配置Hosts. 一般是通过本地切换switchhosts来实现。每个开发都需要保留一份文件，很低效。
通过dnsserver，就可以集中管理，维护一份即可。

启动类：
java -jar ***.jar -DDNS.SERVER=172.16.80.104,172.16.80.105 -DDNS.PORT=53

DNS.SERVER： 公共dns服务器IP
DNS.PORT：  公共dns服务器端口，默认53


客户端使用：
开发机只需要配置dns服务器到这台服务即可。（这台服务需要连接外网，外部域名还需要通过公共dns解析）

查询解析结果：
提供了一个简单的http服务器查看可以解析的host
http://10.131.0.104:8900/?host=www.baidu.com&host=gmail.com 
