package com.ibmteam02.backend_consultation.global.config;

import java.util.List;

// REST(CORS)와 WebSocket에서 함께 사용하는 허용 출처 목록
public final class AllowedOrigins {

    public static final List<String> PATTERNS = List.of(
            "http://localhost:*",
            "http://192.168.*:*",
            "http://medinote-team-frontend.s3-website.ap-northeast-2.amazonaws.com",
            "https://d3s9d84ez4sxzx.cloudfront.net"
    );

    private AllowedOrigins() {
    }
}
