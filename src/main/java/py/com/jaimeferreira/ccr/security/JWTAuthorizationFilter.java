package py.com.jaimeferreira.ccr.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

@Configuration
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    private final String HEADER = "Authorization";
    private final String PREFIX = "Bearer ";
    // clave-privada
    private final String SECRET = "jWmZq4t7w!z%C*F-JaNdRgUkXp2r5u8x/A?D(G+KbPeShVmYq3t6v9y$B&E)H@Mc";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                                                                                                                 throws ServletException,
                                                                                                                 IOException {
        try {
            if (existeJWTToken(request, response)) {
                Claims claims = validateToken(request);
                if (claims.get("authorities") != null) {
                    setUpSpringAuthentication(claims);
                }
                else {
                    SecurityContextHolder.clearContext();
                }
            }
            chain.doFilter(request, response);
        }
        catch (ExpiredJwtException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            LOGGER.error(response.toString());

            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token Expirado");
        }
        catch (UnsupportedJwtException e) {
            e.printStackTrace();

            LOGGER.error(response.toString());
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                               "El Token no es soportado por la Aplicacion");

        }
        catch (MalformedJwtException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            LOGGER.error(response.toString());

            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token mal formado");

        }
    }

    private Claims validateToken(HttpServletRequest request) {
        JwtParserBuilder jwtpb = Jwts.parserBuilder().setSigningKey(SECRET.getBytes());
        return jwtpb.build().parseClaimsJws(request.getHeader(HEADER).replace(PREFIX, "")).getBody();
    }

    public Claims validateTokenString(String token) {
        JwtParserBuilder jwtpb = Jwts.parserBuilder().setSigningKey(SECRET.getBytes());
        return jwtpb.build().parseClaimsJws(token).getBody();
    }

    /**
     * Metodo para autenticarnos dentro del flujo de Spring
     * 
     * @param claims
     */
    private void setUpSpringAuthentication(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> authorities = (List<String>) claims.get("authorities");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(claims.getSubject(), null,
                                                                                           authorities.stream()
                                                                                                      .map(SimpleGrantedAuthority::new)
                                                                                                      .collect(Collectors.toList()));
        SecurityContextHolder.getContext().setAuthentication(auth);

    }

    private boolean existeJWTToken(HttpServletRequest request, HttpServletResponse res) {
        String authenticationHeader = request.getHeader(HEADER);
        if (authenticationHeader == null || !authenticationHeader.startsWith(PREFIX)) {
            return false;
        }
        return true;
    }

}
