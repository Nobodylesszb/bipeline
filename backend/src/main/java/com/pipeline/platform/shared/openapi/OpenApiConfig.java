package com.pipeline.platform.shared.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Pipeline CI/CD Platform API",
                version = "0.1.0",
                description = "自研 CI/CD 平台后端 API。当前阶段重点打通 GitLab/Gitea 代码源连接、连通性验证、后续 Project/Pipeline 创建。",
                contact = @Contact(name = "bo")
        ),
        servers = {
                @Server(url = "http://localhost:8100", description = "本地开发环境")
        }
)
public class OpenApiConfig {
}
