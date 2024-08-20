package com.fluffytime.user.interceptor;

import com.fluffytime.auth.jwt.util.JwtTokenizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@RequiredArgsConstructor
public class LoginCheckInterceptor implements HandlerInterceptor {

    private final JwtTokenizer jwtTokenizer;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
        Object handler) throws Exception {

        String accessToken = jwtTokenizer.getTokenFromCookie(request, "accessToken");

        if (accessToken == null) {
            return true;
        }
        log.info("이미 로그인된 사용자입니다. 해당 페이지에 접근할 수 없습니다.");
        response.sendRedirect("/");
        return false;
    }
}