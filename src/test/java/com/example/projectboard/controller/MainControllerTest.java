package com.example.projectboard.controller;

import com.example.projectboard.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Import(SecurityConfig.class)
@WebMvcTest(MainController.class)
class MainControllerTest {
    private final MockMvc mvc;

    public MainControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void givenNothing_whenRequestingRootPage_thenRedirectsToArticlesPage() throws Exception {
        // given

        // when & then
        mvc.perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("forward:/articles"))
                .andExpect(MockMvcResultMatchers.forwardedUrl("/articles"))
                .andDo(MockMvcResultHandlers.print());
    }
}
