package com.controller;

import com.config.SocketConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.room.util.entity.User;
import com.room.util.json.JsonObject;
import com.room.util.json.JsonObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@RestController
public class SignUpController {
    private final ShaPasswordEncoder passwordEncoder;

    @Autowired
    public SignUpController(ShaPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @RequestMapping("/user")
    public ModelAndView registration(@RequestParam String username,
                                     @RequestParam String password,
                                     @RequestParam String repeatPassword) throws Exception {
        if (username == null || password == null || repeatPassword == null) {
            return new ModelAndView("redirect:/error");
        }
        if (username.equals("") || password.equals("") || repeatPassword.equals("") || !password.equals(repeatPassword)) {
            return new ModelAndView("redirect:/error");
        }

        User user = signUp(username, password);

        if (user.getLogin() != null && user.getPassword() != null) {
            return new ModelAndView("redirect:/login");
        } else {
            return new ModelAndView("redirect:/error");
        }
    }

    // TODO: 8/9/16 auth set to/from || auth special publisher
    // TODO: 8/9/16 try to launch 2-3 rooms
    private User signUp(String username, String password) throws JsonProcessingException {
        SocketConfig instance = SocketConfig.getInstance();

        User user = new User(username, passwordEncoder.encodePassword(password, null));
        String command = "newUser";
        String json = JsonObjectFactory.getJsonString(command, user);
        instance.send(json);

        String reply = instance.receive();
        instance.close();
        user = Optional.ofNullable(JsonObjectFactory.getObjectFromJson(reply, User.class)).orElseGet(User::new);
        return user;
    }
}
