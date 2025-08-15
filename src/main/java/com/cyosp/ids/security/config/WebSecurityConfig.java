package com.cyosp.ids.security.config;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.security.jwt.JwtAccessDeniedHandler;
import com.cyosp.ids.security.jwt.JwtAuthenticationEntryPoint;
import com.cyosp.ids.security.jwt.JwtConfigurer;
import com.cyosp.ids.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static com.cyosp.ids.rest.authentication.signin.SigninController.SIGNIN_PATH;
import static com.cyosp.ids.rest.authentication.signup.SignupController.SIGNUP_PATH;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;


@EnableWebSecurity
@RequiredArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final IdsConfiguration idsConfiguration;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        List<String> publicPaths = idsConfiguration.areMediasPublicShared() ? of("/**")
                : asList(SIGNUP_PATH + "/**", SIGNIN_PATH);

        httpSecurity
                .csrf().disable()

                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)

                .and()
                .headers()
                .frameOptions()
                .sameOrigin()

                .and()
                .cors()
                .configurationSource(request -> new CorsConfiguration().applyPermitDefaultValues())

                .and()
                .sessionManagement()
                .sessionCreationPolicy(STATELESS)

                .and()
                .authorizeRequests()
                .antMatchers(publicPaths.toArray(String[]::new)).permitAll()
                .anyRequest()
                .authenticated()

                .and()
                .apply(securityConfigurerAdapter());
    }

    private JwtConfigurer securityConfigurerAdapter() {
        return new JwtConfigurer(jwtTokenProvider);
    }
}
