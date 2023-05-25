package com.chaoqwq.gbt32960.codec;

import com.chaoqwq.gbt32960.message.ResponseMessage;
import com.chaoqwq.gbt32960.modle.DataUnit;
import com.chaoqwq.gbt32960.paltform.LoginPlatform;
import com.chaoqwq.gbt32960.paltform.PlatformMessage;
import com.chaoqwq.gbt32960.type.RequestType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static com.chaoqwq.gbt32960.message.GBT32960Message.*;


@Slf4j(topic = "netty")
public class GBT32960Encoder extends MessageToByteEncoder<ResponseMessage> {

    // 从protocalhandler过来的数据为responseMessage，会被此encoder读取
    @Override
    protected void encode(ChannelHandlerContext ctx, ResponseMessage response, ByteBuf out) {

        RequestType requestType = response.getMessage().getRequestType();

        switch (requestType) {
            case LOGIN:
                encodeLoginMessage(out, response.getVin(), requestType, response.getMessage().getResponseTag());
                break;
            case LOGOUT:
                encodeLogoutMessage(out, response.getVin(), requestType, response.getMessage().getResponseTag());
                break;
            case PLATFORM_LOGIN:
                encodeMessage(out, response.getVin(), requestType, response.getMessage().getResponseTag(), buf -> encodePlatformLogin(response.getMessage(), buf));
                break;
            case PLATFORM_LOGOUT:
                encodeMessage(out, response.getVin(), requestType, response.getMessage().getResponseTag(), buf -> encodePlatformLogout(response.getMessage(), buf));
                break;
            case REAL_TIME:
            case REISSUE:
            case HEART_BEAT:
                encodeMessage(out, response.getVin(), requestType, response.getMessage().getResponseTag(), buf -> encodeDataUnit(response.getDataUnit(), buf));
                break;
            default:
                log.info("no encode type");
                break;
        }
        log.info("发送gbt32960报文:{}", ByteBufUtil.hexDump(out));

    }

    private void encodeDataUnit(DataUnit dataUnit, ByteBuf out) {
        if (dataUnit == null) {
            return;
        }
        ReportEncoder.encodeFully(dataUnit, out);
    }


    private void encodePlatformLogin(PlatformMessage platformMessage, ByteBuf out) {

        //登入信息的真正数据
        LoginPlatform data = platformMessage.getData();
        if (data == null) {
            return;
        }
        //平台登入的数据采集时间需要满足国标要求,writeTime方法已经进行实现
        writeTime(out, System.currentTimeMillis() / 1000);
        //登入流水号占两个字节
//        下级平台每登入一次,登入流水号自动加1,从1开始循环累加,最大值为65531,循环周期为天
        out.writeShort(data.getLoginDaySeq());
        //平台用户名
        out.writeCharSequence(data.getUsername(), ASCII_CHARSET);
        //平台密码
        out.writeCharSequence(data.getPassword(), ASCII_CHARSET);
        //平台加密规则
        /*0x01:数据不加密;0x02:数据经过RSA算法加密;0x03:数据经过AES128位算法加密;
        “0xFE”表示异常,“0xFF”表示无效,其他预留*/
        out.writeByte(data.getEncryption());
    }

    private void encodePlatformLogout(PlatformMessage platformMessage, ByteBuf out) {
        //平台登出的数据
        LoginPlatform data = platformMessage.getData();
        if (data == null) {
            return;
        }
        //平台登出的数据采集时间需要满足国标要求,writeTime方法已经进行实现
        writeTime(out, System.currentTimeMillis() / 1000);
        //登入流水号占两个字节
//        下级平台每登入一次,登入流水号自动加1,从1开始循环累加,最大值为65531,循环周期为天
        out.writeShort(data.getLoginDaySeq());
        //平台用户名
        out.writeCharSequence(data.getUsername(), ASCII_CHARSET);
        //平台密码
        out.writeCharSequence(data.getPassword(), ASCII_CHARSET);
        //平台加密规则
        /*0x01:数据不加密;0x02:数据经过RSA算法加密;0x03:数据经过AES128位算法加密;
        “0xFE”表示异常,“0xFF”表示无效,其他预留*/
        out.writeByte(data.getEncryption());
    }


      

}
