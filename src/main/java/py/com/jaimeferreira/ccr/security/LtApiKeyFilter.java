package py.com.jaimeferreira.ccr.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Configuration
public class LtApiKeyFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LtApiKeyFilter.class);
    private static final Logger AUDIT  = LoggerFactory.getLogger("lt.audit");
    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${lt.api.key}")
    private String ltApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!request.getServletPath().startsWith("/lt/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (ltApiKey.equals(token)) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "lt-provider", null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_LT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                doFilterConAuditoria(request, response, chain);
                return;
            }
        }

        LOGGER.warn("LT API key inválida o ausente para: {}", request.getServletPath());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"ERROR\",\"mensaje\":\"API key inválida\",\"registros\":0}");
    }

    /**
     * Deja rastro del request al inicio y al fin (con duración y status), incluso si falla
     * o queda colgado: el audit del controller solo escribe cuando el request termina OK.
     */
    private void doFilterConAuditoria(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ruta = request.getMethod() + " " + request.getRequestURI();
        String ip = clientIp(request);
        long inicio = System.currentTimeMillis();
        AUDIT.info("INICIO {} | ip={} | bytes={}", ruta, ip, request.getContentLengthLong());
        try {
            chain.doFilter(request, response);
            AUDIT.info("FIN {} | ip={} | http={} | ms={}", ruta, ip, response.getStatus(),
                    System.currentTimeMillis() - inicio);
        } catch (IOException | ServletException | RuntimeException e) {
            AUDIT.error("FALLO {} | ip={} | ms={} | error={}: {}", ruta, ip,
                    System.currentTimeMillis() - inicio, e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isEmpty())
               ? forwarded.split(",")[0].trim()
               : request.getRemoteAddr();
    }
}
