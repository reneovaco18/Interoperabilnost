    package com.interoperability.aliexpressproject.config;

    import com.interoperability.aliexpressproject.security.JwtFilter;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

    import static org.springframework.security.config.Customizer.withDefaults;

    @EnableWebSecurity
    @Configuration
    public class SecurityConfig {


        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return (web) -> web.ignoring()
                    .requestMatchers(
                            new AntPathRequestMatcher("/services/**")
                    );
        }


        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http, JwtFilter jwt) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())

                    .authorizeHttpRequests(auth -> auth

                            .requestMatchers(
                                    new AntPathRequestMatcher("/auth/**"),
                                    new AntPathRequestMatcher("/api/aliproducts/upload/xsd"),
                                    new AntPathRequestMatcher("/api/aliproducts/upload/rng"),
                                    new AntPathRequestMatcher("/xmlrpc"),
                                    new AntPathRequestMatcher("/admin/seed"),
                                    new AntPathRequestMatcher("/services"),
                                    new AntPathRequestMatcher("/services/**")

                            ).permitAll()


                            .requestMatchers(
                                    new AntPathRequestMatcher("/api/aliproducts/**")
                            ).authenticated()


                            .anyRequest().authenticated()
                    )


                    .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)


                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))


                    .httpBasic(withDefaults());

            return http.build();
        }
    }
