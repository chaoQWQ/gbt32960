package com.chaoqwq.gbt32960.codec;

import com.chaoqwq.gbt32960.modle.DataUnit;
import com.chaoqwq.gbt32960.modle.Location;
import com.chaoqwq.gbt32960.type.RealTimeType;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import static com.chaoqwq.gbt32960.message.GBT32960Message.writeTime;

@Slf4j(topic = "netty")
public class ReportEncoder {

    public static void encodeFully(DataUnit dataUnit, ByteBuf out) {
        //写入数据采集时间
        writeTime(out, System.currentTimeMillis() / 1000);
        //处理写入实时上报信息
        if (dataUnit.getLocation() != null) {
            encodeVehicleLocation(dataUnit.getLocation(),out);
        }
        //todo 其它类型车辆数据处理
    }


    /**
     * 编码车辆位置数据
     */
    private static void encodeVehicleLocation(Location location, ByteBuf out) {
        out.writeByte(RealTimeType.LOCATION.getValue());
        byte mark = (byte) ((location.isGpsStatus() ? 0 : 1) + (location.getGpsLatitude() > 0 ? 1 << 1 : 0) + (location.getGpsLongitude() > 0 ? 1 << 2 : 0));
        out.writeByte(mark);
        out.writeDouble(location.getGpsLongitude()*1000000.0);
        out.writeDouble(location.getGpsLongitude()*1000000.0);
    }

}
