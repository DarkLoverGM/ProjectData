package com.yt.backend.common;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * IP cua client giong req.ip ben Node voi app.set('trust proxy', 'loopback'):
 * chi tin X-Forwarded-For khi request den tu nginx tren cung may (nginx/hello-react.conf);
 * client goi thang vao port 3001 thi van la IP that, khong gia header duoc.
 */
public final class ClientIp {

    private static final String ATTR = ClientIp.class.getName();

    private ClientIp() {}

    public static String of(HttpServletRequest req) {
        Object cached = req.getAttribute(ATTR);
        if (cached instanceof String s) return s;
        String ip = resolve(req.getRemoteAddr(), req.getHeader("X-Forwarded-For"));
        req.setAttribute(ATTR, ip);
        return ip;
    }

    /**
     * Giong proxy-addr: di tu dia chi socket nguoc ve dau X-Forwarded-For,
     * dung o dia chi dau tien khong phai loopback (deu la loopback thi lay dia chi cuoi cung).
     */
    static String resolve(String remoteAddr, String forwardedFor) {
        List<String> addrs = new ArrayList<>();
        addrs.add(remoteAddr);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String p = parts[i].trim();
                if (!p.isEmpty()) addrs.add(p);
            }
        }
        for (String addr : addrs) {
            if (!isLoopback(addr)) return addr;
        }
        return addrs.get(addrs.size() - 1);
    }

    private static boolean isLoopback(String addr) {
        if (addr == null || addr.isEmpty()) return false;
        String a = addr.startsWith("::ffff:") ? addr.substring(7) : addr;
        // Chi nhan dang IP so, khong tra DNS
        if (!a.matches("\\d{1,3}(\\.\\d{1,3}){3}") && !a.matches("[0-9a-fA-F:]*:[0-9a-fA-F:]*")) return false;
        try {
            return InetAddress.getByName(a).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
