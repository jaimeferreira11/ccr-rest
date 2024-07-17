package py.com.jaimeferreira.ccr.security;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTAuthorizationUtils {
    
    @Value("${jwt.key.private}")
    private String secretKey;

    @Value("${jwt.key.time.millis}")
    private int tiempoDeVidaDelToken;
    
    public String getJWTToken(String username) {
        List<GrantedAuthority> grantedAuthorities = AuthorityUtils.commaSeparatedStringToAuthorityList("USER");
        String token = Jwts.builder().setId("token").setSubject(username)
                           .claim("authorities",
                                  grantedAuthorities.stream().map(GrantedAuthority::getAuthority)
                                                    .collect(Collectors.toList()))
                           .setIssuedAt(new Date(System.currentTimeMillis()))
//                           .setExpiration(new Date(System.currentTimeMillis() + tiempoDeVidaDelToken))
                           .signWith(this.getSigningKey()).compact();
        return token;
    }

    private Key getSigningKey() {
        byte[] keyBytes = this.secretKey.getBytes();
        Keys.secretKeyFor(SignatureAlgorithm.HS512);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
