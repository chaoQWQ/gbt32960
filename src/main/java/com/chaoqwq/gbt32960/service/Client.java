package com.chaoqwq.gbt32960.service;

import com.chaoqwq.gbt32960.codec.GBT32960Decoder;
import com.chaoqwq.gbt32960.codec.GBT32960Encoder;
import com.chaoqwq.gbt32960.message.ResponseMessage;
import com.chaoqwq.gbt32960.paltform.LoginPlatform;
import com.chaoqwq.gbt32960.paltform.PlatformMessage;
import com.chaoqwq.gbt32960.protocol.ProtocolHandler;
import com.chaoqwq.gbt32960.type.EncryptionType;
import com.chaoqwq.gbt32960.type.RequestType;
import com.chaoqwq.gbt32960.type.ResponseTag;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Client {

    private static final String host = "127.0.0.1";
    private static final int port = 11482;
    private static Channel channel;
    private NioEventLoopGroup worker = new NioEventLoopGroup();
    private Bootstrap bootstrap;

    public static void main(String[] args) {
        Client  client = new Client();

        client.start();

        client.sendData();
    }

    private void start() {
        bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
//                        ch.pipeline().addLast(new IdleStateHandler(0,0,5));
                        ch.pipeline().addLast(new GBT32960Decoder());
                        ch.pipeline().addLast(new GBT32960Encoder());
                        ch.pipeline().addLast(ProtocolHandler.getInstance());
                    }
                });
        doConnect();
    }

    /**
     * 连接服务端 and 重连
     */
    protected void doConnect() {

        if (channel != null && channel.isActive()){
            return;
        }
        ChannelFuture connect = bootstrap.connect(host, port);
        //实现监听通道连接的方法
        connect.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {

                if(channelFuture.isSuccess()){
                    channel = channelFuture.channel();
                    System.out.println("连接服务端成功");
                }else{
                    System.out.println("每隔2s重连....");
                    channelFuture.channel().eventLoop().schedule(new Runnable() {

                        @Override
                        public void run() {
                            doConnect();
                        }
                    },2,TimeUnit.SECONDS);
                }
            }
        });
    }
    /**
     * 向服务端发送消息
     */
    private void sendData() {
        Scanner sc= new Scanner(System.in);
        while (sc.hasNextLine()){
            String nextLine = sc.nextLine();
            if (channel != null && channel.isActive()) {
                //获取一个键盘扫描器
                ResponseMessage responseMessage = null;
                if ("1".equals(nextLine)) {
                    responseMessage = sendLoginMsg();
                }
                if ("2".equals(nextLine)) {
                    responseMessage = sendCarLocationMsg();
                }
                if ("3".equals(nextLine)) {
                    responseMessage = sendLogoutMsg();
                }
                channel.writeAndFlush(responseMessage);
            } else {
                log.info("连接已断开或失效");
            }
        }
    }
    public ResponseMessage sendLoginMsg()  {
        log.info("发送登入消息");
        String VIN = "SMSTEST0000000001";
        ResponseMessage responseMessage = new ResponseMessage();
        PlatformMessage message = new PlatformMessage();
        message.setRequestType(RequestType.PLATFORM_LOGIN);
        message.setResponseTag(ResponseTag.COMMAND);
        LoginPlatform data = LoginPlatform.newBuilder()
                .setLoginDaySeq(1)
                .setUsername("test12345678")
                .setPassword("12345678912345678912")
                .setEncryption(EncryptionType.PLAIN.getValue())
                .build();
        message.setData(data);
        responseMessage.setVin(VIN);
        responseMessage.setMessage(message);
        return responseMessage;
    }
    public ResponseMessage sendCarLocationMsg() {
        log.info("发送位置消息");
        String VIN = "100000VIN00000005";
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setVin(VIN);
        PlatformMessage message = new PlatformMessage();
        message.setRequestType(RequestType.REAL_TIME);
        message.setResponseTag(ResponseTag.COMMAND);
        responseMessage.setMessage(message);
        return responseMessage;

    }

    public ResponseMessage sendLogoutMsg() {
        log.info("发送登出消息");
        String VIN = "100000VIN00000005";
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setVin(VIN);
        PlatformMessage message = new PlatformMessage();
        message.setRequestType(RequestType.PLATFORM_LOGOUT);
        message.setResponseTag(ResponseTag.COMMAND);
        LoginPlatform data = LoginPlatform.newBuilder()
                .setLoginDaySeq(200)
                .setUsername("fengye202303")
                .setPassword("e59586e6881111111111")
                .setEncryption(EncryptionType.PLAIN.getValue())
                .build();
        message.setData(data);
        responseMessage.setMessage(message);
        return responseMessage;
    }

}
