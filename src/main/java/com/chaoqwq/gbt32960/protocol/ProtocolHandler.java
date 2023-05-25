package com.chaoqwq.gbt32960.protocol;


import com.alibaba.fastjson.JSON;
import com.chaoqwq.gbt32960.formatters.FormatterToDataUnit;
import com.chaoqwq.gbt32960.formatters.TimeFormat;
import com.chaoqwq.gbt32960.message.*;
import com.chaoqwq.gbt32960.modle.DataUnit;
import com.chaoqwq.gbt32960.paltform.FrameHeader;
import com.chaoqwq.gbt32960.paltform.LoginPlatform;
import com.chaoqwq.gbt32960.paltform.PlatformMessage;
import com.chaoqwq.gbt32960.type.RequestType;
import com.chaoqwq.gbt32960.type.ResponseTag;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static com.chaoqwq.gbt32960.type.RequestType.*;


/**
 * @author zuochao
 */
@Slf4j(topic = "netty")
@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

    private static final String USERNAME = "test12345678";
    private static final String PASSWORD = "12345678912345678912";

    public static AttributeKey<Boolean> LOGIN_STATUS = AttributeKey.valueOf("platformLogin");

    @Getter
    private static final ProtocolHandler instance = new ProtocolHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        response(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        log.info("{}连接已经断开",ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("发送错误{}连接将断开",ctx.channel(),cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                log.info("读空闲，断开连接");
                ctx.close();
            }
            if (e.state() == IdleState.WRITER_IDLE) {
//                log.info("写空闲，断开连接");
//                ctx.close();
                //客户端添加心跳逻辑
                sendHeartBeat(ctx);
            }
        }

    }

    private void sendHeartBeat(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(responseMessage("11111111111111111", RequestType.HEART_BEAT, ResponseTag.COMMAND));
    }

    private void platformLoginResponse(ChannelHandlerContext context, Object msg) {
        // 获取平台响应登录响应，转换为国标形式数据包
        GBT32960Message message = toGBT32960Message(msg);
        // 获取数据头部信息
        FrameHeader header = message.getHeader();
        log.info("header: ==> " + header);
        // 拿出平台登入的数据单元的用户名和密码结合jpa去寻找postgre上是否有相关数据
        LoginPlatform loginPlatform = (LoginPlatform) message.getDataUnit();

        String username = loginPlatform.getUsername();
        String password = loginPlatform.getPassword();
        boolean exist = USERNAME.equals(username) && PASSWORD.equals(password);
        if (exist) {
            // 标记当前连接已经登陆过
            context.channel().attr(LOGIN_STATUS).set(true);

            context.writeAndFlush(responseMessage(header.getVin(), PLATFORM_LOGIN, ResponseTag.SUCCESS));
            log.info("Platform login success! login time: ==> "
                    + TimeFormat.longTimeToZoneDateTime(loginPlatform.getLoginTime()));
        } else {
            context.writeAndFlush(responseMessage(header.getVin(), PLATFORM_LOGIN, ResponseTag.FAILED));
            log.trace("Platform username or password error!");
        }

    }

    private GBT32960Message toGBT32960Message(Object msg) {
        if (msg instanceof GBT32960Message) {
            return (GBT32960Message) msg;
        }
        throw new ClassCastException("msg not conversion to GBT32960Message");
    }

    private void response(ChannelHandlerContext context, Object msg) {
        // 服务端是从decoder刷写过来的 客户端是从encoder刷写过来的
        GBT32960Message message = toGBT32960Message(msg);
        // 获取头部信息（不含##）
        FrameHeader header = message.getHeader();
        // 校验是否登录过
        if (!header.getRequestType().equals(PLATFORM_LOGIN)) {
            Boolean login = context.channel().attr(LOGIN_STATUS).get();
            if(login == null || !login){
                context.close();
            }
        }
        // 对服务端来说：获取请求类型，根据不同请求类型进行不同的刷写给客户端
        switch (header.getRequestType()) {
            case LOGIN:
                loginResponse(context,msg);
                break;
            case LOGOUT:
                logoutResponse(context,msg);
                break;
            case REISSUE:
                reIssueResponse(context, msg);
                break;
            case REAL_TIME:
                realTimeResponse(context, msg);
                break;
            case PLATFORM_LOGIN:
                platformLoginResponse(context, msg);
                break;
            case PLATFORM_LOGOUT:
                platformLogoutResponse(context, msg);
                break;
            case HEART_BEAT:
                heartBeatResponse(context, msg);
                break;
            default:
                log.info("no request type");
                break;
        }
    }

    private void logoutResponse(ChannelHandlerContext context, Object msg) {
        GBT32960Message message = toGBT32960Message(msg);
        LogoutRequest dataUnit = (LogoutRequest) message.getDataUnit();
        System.out.println(dataUnit.toString());
        String vin = message.getHeader().getVin();
        // 写回客户端进行响应
        context.writeAndFlush(responseMessage(vin, RequestType.LOGOUT, ResponseTag.SUCCESS));
    }

    private void heartBeatResponse(ChannelHandlerContext context, Object msg) {
        GBT32960Message message = toGBT32960Message(msg);
        String vin = message.getHeader().getVin();
        // 写回客户端进行响应
        context.writeAndFlush(responseMessage(vin, RequestType.HEART_BEAT, ResponseTag.SUCCESS));
    }

    private void loginResponse(ChannelHandlerContext context, Object msg) {
        GBT32960Message message = toGBT32960Message(msg);
        LoginRequest dataUnit = (LoginRequest) message.getDataUnit();
        System.out.println(dataUnit.toString());
        String vin = message.getHeader().getVin();
        // 写回客户端进行响应
        context.writeAndFlush(responseMessage(vin, RequestType.LOGIN, ResponseTag.SUCCESS));
    }

    private void realTimeResponse(ChannelHandlerContext context, Object msg) {
        GBT32960Message message = toGBT32960Message(msg);
        FrameHeader header = message.getHeader();
        String vin = header.getVin();
        boolean vinExist = true;
        if (vinExist) {
            RealTimeReport realTimeReport = (RealTimeReport) message.getDataUnit();
            DataUnit dataUnit = FormatterToDataUnit.RealTimeReportToDataUnit(realTimeReport, header);
            //todo 数据处理
            log.info("数据获取成功，发送到消息处理队列。数据：location={},vehicle={}" + dataUnit.getLocation().toString(), dataUnit.getVehicle().toString());
            // 写回客户端进行响应
            context.writeAndFlush(responseMessage(vin, RequestType.REAL_TIME, ResponseTag.SUCCESS));
        } else {
            log.info("上报信息vin未登录：[{}]" + vin);
            context.writeAndFlush(responseMessage(vin, RequestType.REAL_TIME, ResponseTag.FAILED));
        }

    }

    private void reIssueResponse(ChannelHandlerContext context, Object msg) {
        GBT32960Message message = toGBT32960Message(msg);
        String vin = message.getHeader().getVin();
        // 写回客户端进行响应
        context.writeAndFlush(responseMessage(vin, RequestType.REISSUE, ResponseTag.SUCCESS));
    }

    private void platformLogoutResponse(ChannelHandlerContext context, Object msg) {
        log.info("退出登录：[{}]" , context.channel());
        context.close();
    }


    private ResponseMessage responseMessage(String vin, RequestType requestType, ResponseTag responseTag) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setVin(vin);
        PlatformMessage platformMessage = new PlatformMessage();
        platformMessage.setRequestType(requestType);
        platformMessage.setResponseTag(responseTag);
        platformMessage.setData(null);
        responseMessage.setMessage(platformMessage);
        log.info("create response:{}", JSON.toJSONString(responseMessage));
        return responseMessage;
    }


}
