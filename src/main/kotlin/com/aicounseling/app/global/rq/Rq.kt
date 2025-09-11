package com.aicounseling.app.global.rq

import com.aicounseling.app.domain.user.entity.User
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.WebApplicationContext

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
class Rq {
    val authentication
        get() = SecurityContextHolder.getContext().authentication

    val isAuthenticated: Boolean
        get() =
            authentication?.isAuthenticated == true &&
                authentication.principal != "anonymousUser"

    val currentUserId: Long?
        get() =
            if (isAuthenticated) {
                authentication?.name?.toLongOrNull()
            } else {
                null
            }

    // OAuth2 UserService 구현 시 활용 예정
    // OAuth2 로그인 시 OAuth2User를 User 엔티티로 변환하여 사용
    val currentUser: User?
        get() =
            if (isAuthenticated) {
                authentication?.principal as? User
            } else {
                null
            }
}
