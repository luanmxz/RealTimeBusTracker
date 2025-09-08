package com.devluanmarcene.NextBusRealTimeTracker.helpers;

import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;

public class HttpHelper {

    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");

        if (ip != null) {
            ip = new StringTokenizer(ip, "").nextToken().trim();
            System.out.println(String.format("IP Address: %s", ip));
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
