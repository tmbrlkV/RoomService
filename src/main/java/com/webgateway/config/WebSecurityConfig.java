package com.webgateway.config;

import com.chat.util.entity.User;
import com.chat.util.json.JsonObjectFactory;
import com.chat.util.json.JsonProtocol;
import com.webgateway.config.socket.zmq.SocketConfig;
import com.webgateway.entity.CustomUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.Optional;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
    private final ShaPasswordEncoder passwordEncoder;
    private final SocketConfig<String> socketConfig;

    @Autowired
    public WebSecurityConfig(@Qualifier("databaseSocketConfig") SocketConfig<String> socketConfig,
                             ShaPasswordEncoder passwordEncoder) {
        this.socketConfig = socketConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers("/user", "/registration", "/resources/**").anonymous()
                .antMatchers("/", "/topic", "/hello", "/topic/greetings", "/init", "/logout").authenticated()
                .anyRequest().authenticated()
                .and().formLogin().loginPage("/login").defaultSuccessUrl("/init", true).permitAll()
                .and().logout()
                .permitAll();
        http.sessionManagement().invalidSessionUrl("/login");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(username -> {
            User reply = getAuth(username);
            org.springframework.security.core.userdetails.User user = new org.springframework.security
                    .core.userdetails.User(reply.getLogin(), reply.getPassword(), AuthorityUtils.createAuthorityList("USER"));
            return new CustomUser(reply.getId(), user);
        }).passwordEncoder(passwordEncoder);
    }


    private User getAuth(String username) {
        User user = new User(username);
        String command = "getUserByLogin";
        JsonProtocol<User> protocol = new JsonProtocol<>(command, user);
        protocol.setFrom("");
        protocol.setTo("database");
        String json = JsonObjectFactory.getJsonString(protocol);

        logger.debug(json);
        socketConfig.send(json);
        String reply = socketConfig.receive();
        user = (User) Optional.ofNullable(JsonObjectFactory.getObjectFromJson(reply, JsonProtocol.class))
                .map(JsonProtocol::getAttachment)
                .orElseGet(() -> new User("log", "pass"));
        return user;
    }
}