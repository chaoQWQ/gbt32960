package com.chaoqwq.gbt32960.service;


import com.chaoqwq.gbt32960.codec.GBT32960Decoder;
import com.chaoqwq.gbt32960.codec.GBT32960Encoder;
import com.chaoqwq.gbt32960.protocol.ProtocolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * gbt32960协议
 * @author zuochao
 */
@Slf4j(topic = "netty")
public class NettyServer {

    private static final Integer LISTEN_PORT = 11482;

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup);
            serverBootstrap.channel(NioServerSocketChannel.class);

            Bootstrap clientBoot = new Bootstrap();
            clientBoot.group(workerGroup);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new GBT32960Decoder());
                    ch.pipeline().addLast(new GBT32960Encoder());
                    ch.pipeline().addLast(new IdleStateHandler(60, 0, 0));
                    ch.pipeline().addLast(ProtocolHandler.getInstance());
                }
            });
            serverBootstrap.option(ChannelOption.SO_BACKLOG, 128);

            ChannelFuture f = serverBootstrap.bind(LISTEN_PORT).sync();
            log.info("netty server listened on {}", LISTEN_PORT);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

}
