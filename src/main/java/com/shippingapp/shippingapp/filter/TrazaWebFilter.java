package com.shippingapp.shippingapp.filter;

import com.shippingapp.shippingapp.context.TrazaContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrazaWebFilter implements WebFilter {

    private static final String HEADER_TRAZA = "X-Traza-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String trazaId = exchange.getRequest().getHeaders().getFirst(HEADER_TRAZA);
        if (trazaId == null || trazaId.isBlank()) {
            trazaId = UUID.randomUUID().toString();
        }

        String trazaFinal = trazaId;
        exchange.getResponse().getHeaders().set(HEADER_TRAZA, trazaFinal);

        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(TrazaContext.TRAZA_ID_KEY, trazaFinal));
    }
}
