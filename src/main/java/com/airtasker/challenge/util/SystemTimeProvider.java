package com.airtasker.challenge.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
@Component
public class SystemTimeProvider implements TimeProvider {

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now(ZoneOffset.UTC);

    }


}
