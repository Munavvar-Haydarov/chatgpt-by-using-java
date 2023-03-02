package com.unfbx.chatgpt.entity.whisper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * 描述：语音转文字
 *
 * @author https:www.unfbx.com
 * @since  2023-03-02
 */
@Data
public class Whisper {


    @Getter
    @AllArgsConstructor
    public enum Model{
        WHISPER_1("whisper-1"),
        ;
        private String name;
    }
}
