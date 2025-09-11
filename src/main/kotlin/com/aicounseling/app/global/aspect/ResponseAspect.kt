package com.aicounseling.app.global.aspect

import com.aicounseling.app.global.rsData.RsData
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
@Profile("!test & !dev") // test와 dev 프로필에서 비활성화 (prod에서만 활성화)
class ResponseAspect {
    companion object {
        private const val DEFAULT_STATUS_CODE = 200
        private const val MIN_HTTP_STATUS_CODE = 100
        private const val MAX_HTTP_STATUS_CODE = 599

        private val STATUS_CODE_MAP =
            mapOf(
                "200" to HttpStatus.OK,
                "201" to HttpStatus.CREATED,
                "204" to HttpStatus.NO_CONTENT,
                "400" to HttpStatus.BAD_REQUEST,
                "401" to HttpStatus.UNAUTHORIZED,
                "403" to HttpStatus.FORBIDDEN,
                "404" to HttpStatus.NOT_FOUND,
                "409" to HttpStatus.CONFLICT,
                "422" to HttpStatus.UNPROCESSABLE_ENTITY,
                "500" to HttpStatus.INTERNAL_SERVER_ERROR,
                "502" to HttpStatus.BAD_GATEWAY,
                "503" to HttpStatus.SERVICE_UNAVAILABLE,
            )
    }

    @Around(
        "@within(org.springframework.web.bind.annotation.RestController) && " +
            "execution(* com.aicounseling.app..*Controller.*(..)) && " +
            "!execution(* com.aicounseling.app.global.auth.controller.DevAuthController.*(..))",
    )
    fun handleResponse(joinPoint: ProceedingJoinPoint): Any? {
        val result = joinPoint.proceed()

        return when (result) {
            is RsData<*> -> {
                val httpStatus = getHttpStatusFromCode(result.resultCode)
                setResponseStatus(httpStatus)
                ResponseEntity.status(httpStatus).body(result)
            }
            is ResponseEntity<*> -> result
            else -> result
        }
    }

    private fun getHttpStatusFromCode(code: String): HttpStatus {
        // S-로 시작하는 성공 코드 처리
        if (code.startsWith("S-")) {
            val statusCode = code.removePrefix("S-")
            // S-204는 특별히 NO_CONTENT로 처리
            if (statusCode == "204") {
                return HttpStatus.NO_CONTENT
            }
            // 그 외 S- 코드는 200 OK
            return HttpStatus.OK
        }

        // F-로 시작하는 실패 코드 처리
        if (code.startsWith("F-")) {
            val statusCode = code.removePrefix("F-")
            return STATUS_CODE_MAP[statusCode] ?: HttpStatus.INTERNAL_SERVER_ERROR
        }

        // 숫자만 있는 경우 직접 매핑
        return STATUS_CODE_MAP[code] ?: run {
            val numericCode = code.toIntOrNull() ?: DEFAULT_STATUS_CODE
            // HTTP 상태 코드 유효 범위 체크
            if (numericCode in MIN_HTTP_STATUS_CODE..MAX_HTTP_STATUS_CODE) {
                HttpStatus.valueOf(numericCode)
            } else {
                // 범위를 벗어난 경우 기본값 반환
                HttpStatus.OK
            }
        }
    }

    private fun setResponseStatus(status: HttpStatus) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes is ServletRequestAttributes) {
            val response = requestAttributes.response
            response?.status = status.value()
        }
    }
}
