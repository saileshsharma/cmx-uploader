package com.cb.th.claims.cmxuploader.controller;

import java.time.Duration;

public interface SignedUrlService {
    String signGet(String bucket, String objectKey, Duration duration);
}