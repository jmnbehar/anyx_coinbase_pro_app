package com.anyexchange.anyx.classes


enum class ErrorCode(val code: Int) {
    BadRequest(400), //Invalid request format
    Unauthorized(401),
    Forbidden(403),
    NotFound(404),
    TooManyRequests(429),
    ServerError(500), //Problem with our server
    ServiceUnavailable(503), //Problem with our server
    UnknownError(999),
    NoInternet(-1);

    companion object {
        fun withCode(code: Int): ErrorCode {
            return when (code) {
                400 -> BadRequest//Invalid request format
                401 -> Unauthorized
                403 -> Forbidden
                404 -> NotFound
                429 -> TooManyRequests
                500 -> ServerError //Problem with our server
                503 -> ServiceUnavailable //Problem with our server
                else -> UnknownError
            }
        }
    }
}
