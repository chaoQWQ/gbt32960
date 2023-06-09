package com.chaoqwq.gbt32960.paltform;

import com.chaoqwq.gbt32960.constant.GBT32960Constant;
import com.chaoqwq.gbt32960.message.GBT32960Message;
import io.netty.buffer.ByteBuf;
import lombok.Data;

import javax.annotation.Nonnull;

import static com.chaoqwq.gbt32960.message.GBT32960Message.ASCII_CHARSET;


@Data
public class LoginPlatform {

    private long loginTime;
    private int loginDaySeq;
    private String username;
    private String password;
    private int encryption;

    private LoginPlatform(Builder builder) {
        setLoginTime(builder.loginTime);
        setLoginDaySeq(builder.loginDaySeq);
        setUsername(builder.username);
        setPassword(builder.password);
        setEncryption(builder.encryption);
    }

    public static LoginPlatform decoderFul(ByteBuf in) {
        return LoginPlatform.newBuilder()
                .setLoginTime(GBT32960Message.readTime(in))
                .setLoginDaySeq(in.readShort())
                .setUsername(in.readCharSequence(GBT32960Constant.USER_NAME_LENGTH, ASCII_CHARSET).toString())
                .setPassword(in.readCharSequence(GBT32960Constant.USER_PASSWORD_LENGTH, ASCII_CHARSET).toString())
                .setEncryption(in.readByte())
                .build();
    }

    public static LoginPlatform decoderLogout(ByteBuf in) {
        return LoginPlatform.newBuilder()
                .setLoginTime(GBT32960Message.readTime(in))
                .setLoginDaySeq(in.readShort())
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private long loginTime;
        private int loginDaySeq;
        private String username;
        private String password;
        private int encryption;

        private Builder() {
        }

        @Nonnull
        public Builder setLoginTime(long val) {
            loginTime = val;
            return this;
        }

        @Nonnull
        public Builder setLoginDaySeq(int val) {
            loginDaySeq = val;
            return this;
        }

        @Nonnull
        public Builder setUsername(@Nonnull String val) {
            username = val;
            return this;
        }

        @Nonnull
        public Builder setPassword(@Nonnull String val) {
            password = val;
            return this;
        }

        @Nonnull
        public Builder setEncryption(int val) {
            encryption = val;
            return this;
        }

        @Nonnull
        public LoginPlatform build() {
            return new LoginPlatform(this);
        }
    }
}
