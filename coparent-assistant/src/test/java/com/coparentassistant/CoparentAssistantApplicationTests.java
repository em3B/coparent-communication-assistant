package com.coparentassistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coparentassistant.ai.AiAnalysisClient;

@SpringBootTest
class CoparentAssistantApplicationTests {

	@MockitoBean 
	private AiAnalysisClient aiAnalysisClient;

	@Test
	void contextLoads() {
	}

}
