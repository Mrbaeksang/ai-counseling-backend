package com.aicounseling.app.global.rsData

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * 표준 응답 포맷
 * 모든 API 응답을 통일된 형식으로 반환
 */
data class RsData<T>(
    val resultCode: String,
    val msg: String,
    val data: T? = null,
) {
    @JsonIgnore
    fun isSuccess(): Boolean = resultCode.startsWith("S-")

    @JsonIgnore
    fun isFail(): Boolean = !isSuccess()

    companion object {
        fun <T> of(
            resultCode: String,
            msg: String,
            data: T? = null,
        ): RsData<T> {
            return RsData(resultCode, msg, data)
        }

        fun <T> success(data: T? = null): RsData<T> {
            return of("S-1", "성공", data)
        }

        fun <T> success(
            msg: String,
            data: T? = null,
        ): RsData<T> {
            return of("S-1", msg, data)
        }

        fun <T> fail(msg: String): RsData<T> {
            return of("F-1", msg, null)
        }

        fun <T> fail(
            resultCode: String,
            msg: String,
        ): RsData<T> {
            return of(resultCode, msg, null)
        }
    }
}
