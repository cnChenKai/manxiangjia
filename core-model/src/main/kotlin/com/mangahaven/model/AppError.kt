package com.mangahaven.model

/**
 * 统一错误模型。
 * 所有模块中的错误都应该映射到这些类型之一。
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    /** 权限被拒绝 */
    class PermissionDenied(
        message: String = "Permission denied",
        cause: Throwable? = null,
    ) : AppError(message, cause)

    /** 文件未找到 */
    class FileNotFound(
        val path: String,
        cause: Throwable? = null,
    ) : AppError("File not found: $path", cause)

    /** 不支持的格式 */
    class UnsupportedFormat(
        val format: String,
        cause: Throwable? = null,
    ) : AppError("Unsupported format: $format", cause)

    /** 损坏的压缩包 */
    class CorruptedArchive(
        val path: String,
        cause: Throwable? = null,
    ) : AppError("Corrupted archive: $path", cause)

    /** 网络不可用 */
    class NetworkUnavailable(
        message: String = "Network unavailable",
        cause: Throwable? = null,
    ) : AppError(message, cause)

    /** 认证失败 */
    class AuthFailed(
        message: String = "Authentication failed",
        cause: Throwable? = null,
    ) : AppError(message, cause)

    /** 超时 */
    class Timeout(
        message: String = "Operation timed out",
        cause: Throwable? = null,
    ) : AppError(message, cause)

    /** 未知错误 */
    class Unknown(
        message: String = "An unknown error occurred",
        cause: Throwable? = null,
    ) : AppError(message, cause)
}

/**
 * 统一结果类型别名。
 * 使用 Kotlin 标准库的 Result 类型。
 */
typealias AppResult<T> = Result<T>
