package com.shippingapp.shippingapp.support;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class PostgresTestSupport {

    private PostgresTestSupport() {
    }

    public static boolean disponible() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 5432), 500);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
