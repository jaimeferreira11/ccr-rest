package py.com.jaimeferreira.ccr.insights.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filtro HTTP que intercepta todas las requests y retorna HTTP 503 si la plataforma
 * está suspendida. Se ejecuta antes del filtro JWT.
 *
 * Rutas excluidas: /auth/**, /insights/api/v1/admin/plataforma/** y /public/**.
 *
 * @author Jaime Ferreira
 */
@Component
public class PlataformaStatusFilter extends OncePerRequestFilter {

    @Autowired
    private PlataformaService plataformaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Excluir login, rutas públicas y el endpoint de administración de la plataforma
        return path.contains("/auth/") || path.contains("/public/")
                || path.contains("/insights/api/v1/admin/plataforma");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!plataformaService.isActiva()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // 503
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> body = new HashMap<>();
            body.put("suspendida", true);
            body.put("mensaje", plataformaService.getMensajeSuspension());
            body.put("status", 503);

            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
