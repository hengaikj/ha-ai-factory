package com.hengaikj.ai.factory.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Agent Runtime入口；在HD-002解除前所有真实执行请求失败关闭。 */
@RestController
public class AgentRuntimeController {
    /** M02禁止启动真实Runtime，统一返回可操作的配置未批准冲突。 */
    @PostMapping("/projects/{projectId}/tasks/{taskId}/agent-runs")
    public void requestRun(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId, @PathVariable long taskId,
                           @Valid @RequestBody RunRequest body) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少已验证的OIDC主体");
        throw new ResponseStatusException(HttpStatus.CONFLICT, "CONFIG_NOT_APPROVED：Agent Runtime配置未批准，当前不会启动模型、工具或外部副作用");
    }

    /** M02不产生执行记录，因此不存在的运行记录明确返回404。 */
    @GetMapping("/agent-runs/{runId}")
    public void getRun(@AuthenticationPrincipal OidcUser user, @PathVariable long runId) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少已验证的OIDC主体");
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent执行记录不存在");
    }

    public record RunRequest(@NotNull UUID requestKey, @Size(max = 256) String executionMode) {}
}
