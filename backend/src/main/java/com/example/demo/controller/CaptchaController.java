package com.example.demo.controller;

import com.google.code.kaptcha.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;

@RestController
public class CaptchaController {
    @Autowired
    private Producer captchaProducer;

    // For image-based captcha
    @GetMapping("/captcha-image")
    public void getCaptchaImage(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("image/png");
        String capText = captchaProducer.createText();
        HttpSession session = request.getSession();
        session.setAttribute("captcha", capText);
        BufferedImage bi = captchaProducer.createImage(capText);
        ImageIO.write(bi, "png", response.getOutputStream());
    }
}
