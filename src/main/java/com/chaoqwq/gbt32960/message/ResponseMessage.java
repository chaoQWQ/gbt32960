package com.chaoqwq.gbt32960.message;

import com.chaoqwq.gbt32960.modle.DataUnit;
import com.chaoqwq.gbt32960.paltform.PlatformMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseMessage {
    private String vin;
    private PlatformMessage message;
    private DataUnit dataUnit;
}
