package py.com.jaimeferreira.ccr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import py.com.jaimeferreira.ccr.security.JWTAuthorizationFilter;

/***
 * 
 * @author Luis Capdevila [luis_capde@hotmail.com]
 *
 */

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "py.com.jaimeferreira.ccr")
public class CcrRestApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(CcrRestApplication.class, args);
    }

//    @Override
//    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
//        return application.sources(CcrRestApplication.class);
//    }

//    @Bean
//    SecurityFilterChain webSecurityConfigSecurityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf(csrf -> csrf.disable())
//            .addFilterAfter(new JWTAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
//            .authorizeRequests(authorizeRequests -> authorizeRequests
//                                                                     .antMatchers(HttpMethod.POST,
//                                                                                  "/login",
//                                                                                  "/login/validar-token",
//                                                                                  "/login/encode-password",
//                                                                                  "/login/generate-pdf")
//                                                                     .permitAll()
//                                                                     .anyRequest().authenticated());
//        return http.build();
//    }
    
    
//    @Bean
//    SecurityFilterChain webSecurityConfigSecurityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf(csrf -> csrf.disable())
//            .addFilterAfter(new JWTAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
//            .authorizeRequests(authorizeRequests -> authorizeRequests
//                //.antMatchers("/public/**").permitAll()  
//                //.antMatchers(HttpMethod.POST, "/auth/", "/login/validar-token", "/login/encode-password", "/login/generate-pdf")
//                //.permitAll()
//                .antMatchers("/auth/**").permitAll()  
//                .antMatchers("/api/v1/**").authenticated()  
////                .antMatchers("/api/**").permitAll() 
//                .anyRequest().authenticated());
//        return http.build();
//    }

    

}
