package com.averygrimes.secretschest;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Avery Grimes-Farrow
 * Created on: 6/13/20
 * https://github.com/helloavery
 */

@ActiveProfiles("test")
public class BaseTestConfig {

    @BeforeEach
    public void setUp(){
        MockitoAnnotations.initMocks(this);
    }
}