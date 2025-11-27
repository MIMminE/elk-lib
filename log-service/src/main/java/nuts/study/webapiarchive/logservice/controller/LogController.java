package nuts.study.webapiarchive.logservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/log")
@Slf4j
public class LogController {

    @GetMapping("/info")
    public String getInfo() {
        // 다양한 info 메시지 중 랜덤 선택
        List<String> msgs = List.of(
                "User logged in",
                "Cache refreshed",
                "Scheduled job started",
                "Processed payment",
                "Connection healthy"
        );
        String msg = msgs.get(ThreadLocalRandom.current().nextInt(msgs.size()));

        // 간단한 구조화 필드 추가
        MDC.put("userId", "user-" + ThreadLocalRandom.current().nextInt(1000, 9999));
        Map<String, String> details = Map.of("transactionId", "tx-" + ThreadLocalRandom.current().nextInt(10000, 99999));
        log.info("{} - details={}", msg, details);
        MDC.clear();

        return msg;
    }

    @GetMapping("/error")
    public String getError() {
        // 서로 다른 예외 시나리오를 랜덤으로 발생시켜 로그에 찍음
        int r = ThreadLocalRandom.current().nextInt(3);
        try {
            if (r == 0) {
                throw new IllegalStateException("simulated illegal state");
            } else if (r == 1) {
                throw new NullPointerException("simulated null pointer");
            } else {
                throw new RuntimeException("simulated runtime exception");
            }
        } catch (Exception e) {
            MDC.put("requestId", "req-" + ThreadLocalRandom.current().nextInt(10000, 99999));
            log.error("An error occurred while processing request", e);
            MDC.clear();
        }
        return "error logged";
    }

    @GetMapping("/json")
    public String getJsonLog() {
        // JSON 형식 로그를 랜덤 이벤트/상태로 생성
        String event = ThreadLocalRandom.current().nextBoolean() ? "json_log_accessed" : "json_event_alt";
        String status = ThreadLocalRandom.current().nextBoolean() ? "success" : "partial";
        String id = "id-" + ThreadLocalRandom.current().nextInt(1000, 9999);
        String json = String.format("{\"event\":\"%s\",\"status\":\"%s\",\"id\":\"%s\"}", event, status, id);

        log.info(json);
        return json;
    }

    @GetMapping("/bulk")
    public String bulk(@RequestParam(defaultValue = "100") int n) {
        // 대량 로그 생성 (테스트용)
        for (int i = 0; i < n; i++) {
            String msg = "bulk-msg-" + i + " type=" + (i % 5);
            // 가끔은 경고도 섞음
            if (i % 37 == 0) {
                log.warn("bulk warning: {}", msg);
            } else {
                log.info(msg);
            }
        }
        return "bulk:" + n;
    }
}
