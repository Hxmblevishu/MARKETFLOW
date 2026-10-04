package com.marketflow.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

@Component
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "metadata.google.internal",
            "instance-data",
            "169.254.169.254"
    );

    public void validateUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + urlString, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))) {
            throw new SecurityException("SSRF Protection: Protocol scheme '" + scheme + "' is forbidden. Only HTTP/HTTPS are permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SecurityException("SSRF Protection: Hostname cannot be null or empty in outbound request URL.");
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTNAMES.contains(normalizedHost)) {
            throw new SecurityException("SSRF Protection: Request to internal/metadata host '" + host + "' is strictly prohibited.");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isRestrictedIp(address)) {
                    log.warn("Blocked SSRF attempt targeting restricted IP: {} for host: {}", address.getHostAddress(), host);
                    throw new SecurityException("SSRF Protection: Host '" + host + "' resolves to restricted/internal IP address: " + address.getHostAddress());
                }
            }
        } catch (UnknownHostException ex) {
            // Unresolvable domain (e.g. offline tests or mock services); not a restricted IP literal
            log.debug("DNS resolution unresolvable for outbound host '{}': {}", host, ex.getMessage());
        }
    }

    public boolean isRestrictedIp(InetAddress address) {
        if (address.isLoopbackAddress()) {
            return true; // 127.0.0.0/8, ::1
        }
        if (address.isAnyLocalAddress()) {
            return true; // 0.0.0.0
        }
        if (address.isSiteLocalAddress()) {
            return true; // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        }
        if (address.isLinkLocalAddress()) {
            return true; // 169.254.0.0/16, fe80::/10 (AWS/GCP/Azure instance metadata)
        }
        if (address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 100.64.0.0/10 (Carrier-Grade NAT)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) {
                return true;
            }
            // 192.0.0.0/24 (IETF Protocol Assignments)
            if (b0 == 192 && b1 == 0) {
                return true;
            }
            // 198.18.0.0/15 (Benchmarking)
            if (b0 == 198 && (b1 == 18 || b1 == 19)) {
                return true;
            }
            // 169.254.169.254 explicit check
            if (b0 == 169 && b1 == 254) {
                return true;
            }
        }

        return false;
    }
}
