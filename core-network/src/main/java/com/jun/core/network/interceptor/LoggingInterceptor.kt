package com.jun.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import timber.log.Timber
import java.io.IOException
import java.util.UUID

/**
 * 网络请求日志拦截器
 * 用于记录请求和响应的详细信息
 * 
 * 优化特性：
 * - 请求ID追踪
 * - JSON格式化
 * - 长文本截断
 * - 响应体安全读取（使用peekBody，不消耗响应流）
 */
class LoggingInterceptor(
    private val enabled: Boolean = true,
    private val logLevel: LogLevel = LogLevel.BODY,
    private val formatJson: Boolean = true,
    private val maxBodyLength: Int = 2000
) : Interceptor {
    
    enum class LogLevel {
        NONE,       // 不记录日志
        BASIC,      // 只记录请求方法和URL
        HEADERS,    // 记录请求方法和URL以及请求头
        BODY        // 记录请求方法和URL、请求头以及请求体和响应体
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled || logLevel == LogLevel.NONE) {
            return chain.proceed(chain.request())
        }
        
        val request = chain.request()
        val requestId = UUID.randomUUID().toString().take(8)
        val requestStartTime = System.currentTimeMillis()
        
        // 记录请求信息
        logRequest(request, requestId)
        
        val response: Response
        try {
            response = chain.proceed(request)
            val requestEndTime = System.currentTimeMillis()
            val duration = requestEndTime - requestStartTime
            
            // 记录响应信息
            logResponse(response, duration, requestId)
            
            return response
        } catch (e: IOException) {
            val requestEndTime = System.currentTimeMillis()
            val duration = requestEndTime - requestStartTime
            logError(request, e, duration, requestId)
            throw e
        }
    }
    
    private fun logRequest(request: okhttp3.Request, requestId: String) {
        Timber.tag("Network").d("┌────── Request [$requestId] ──────")
        Timber.tag("Network").d("│ ${request.method} ${request.url}")
        
        if (logLevel == LogLevel.HEADERS || logLevel == LogLevel.BODY) {
            if (request.headers.size > 0) {
                Timber.tag("Network").d("│ Headers:")
                request.headers.forEach { header ->
                    // 隐藏敏感信息
                    val value = if (isSensitiveHeader(header.first)) {
                        "***"
                    } else {
                        header.second
                    }
                    val headerLine = "${header.first}: $value"
                    // 如果请求头过长，自动换行显示
                    if (headerLine.length > 120) {
                        logLongLine(headerLine, maxLineLength = 120, prefix = "│   ")
                    } else {
                        Timber.tag("Network").d("│   $headerLine")
                    }
                }
            }
        }
        
        if (logLevel == LogLevel.BODY && request.body != null) {
            try {
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val requestBody = buffer.readUtf8()
                val formattedBody = formatBody(requestBody, isRequest = true)
                Timber.tag("Network").d("│ Body:")
                logBody(formattedBody)
            } catch (e: Exception) {
                Timber.tag("Network").d("│ Body: [无法读取请求体: ${e.message}]")
            }
        }
        
        // 打印 curl 命令
        if (logLevel == LogLevel.BODY || logLevel == LogLevel.HEADERS) {
            logCurlCommand(request)
        }
    }
    
    private fun logResponse(response: Response, duration: Long, requestId: String) {
        val statusEmoji = when {
            response.code in 200..299 -> "✅"
            response.code in 300..399 -> "⚠️"
            response.code in 400..499 -> "❌"
            response.code >= 500 -> "🔥"
            else -> "❓"
        }
        
        Timber.tag("Network").d("├────── Response [$requestId] $statusEmoji ──────")
        Timber.tag("Network").d("│ ${response.code} ${response.message}")
        Timber.tag("Network").d("│ Duration: ${duration}ms")
        
        if (logLevel == LogLevel.HEADERS || logLevel == LogLevel.BODY) {
            if (response.headers.size > 0) {
                Timber.tag("Network").d("│ Headers:")
                response.headers.forEach { header ->
                    val headerLine = "${header.first}: ${header.second}"
                    // 如果响应头过长，自动换行显示
                    if (headerLine.length > 120) {
                        logLongLine(headerLine, maxLineLength = 120, prefix = "│   ")
                    } else {
                        Timber.tag("Network").d("│   $headerLine")
                    }
                }
            }
        }
        
        if (logLevel == LogLevel.BODY) {
            try {
                // 使用 peekBody 读取响应体，不会消耗响应流
                val responseBody = response.peekBody(maxBodyLength.toLong())
                val responseBodyString = responseBody.string()
                val formattedBody = formatBody(responseBodyString, isRequest = false)
                Timber.tag("Network").d("│ Body:")
                logBody(formattedBody)
                
                // 如果响应体被截断，提示用户
                if (responseBodyString.length >= maxBodyLength && maxBodyLength > 0) {
                    Timber.tag("Network").d("│ [响应体已截断，实际长度: ${response.body?.contentLength() ?: 0} 字节]")
                }
            } catch (e: Exception) {
                Timber.tag("Network").d("│ Body: [无法读取响应体: ${e.message}]")
            }
        }
        Timber.tag("Network").d("└─────────────────────────────────────")
    }
    
    private fun logError(request: okhttp3.Request, e: IOException, duration: Long, requestId: String) {
        Timber.tag("Network").e("┌────── Error [$requestId] ❌ ──────")
        Timber.tag("Network").e("│ ${request.method} ${request.url}")
        Timber.tag("Network").e("│ Duration: ${duration}ms")
        Timber.tag("Network").e("│ Error: ${e.javaClass.simpleName}")
        Timber.tag("Network").e("│ Message: ${e.message}")
        Timber.tag("Network").e("└─────────────────────────────────────")
        Timber.tag("Network").e(e, "网络请求异常详情")
    }
    
    /**
     * 格式化请求体或响应体
     */
    private fun formatBody(body: String, isRequest: Boolean): String {
        if (body.isBlank()) {
            return "[空]"
        }
        
        // 尝试格式化JSON
        if (formatJson && isJson(body)) {
            return try {
                formatJson(body)
            } catch (e: Exception) {
                body
            }
        }
        
        return body
    }
    
    /**
     * 判断是否为JSON格式
     */
    private fun isJson(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }
    
    /**
     * 格式化JSON字符串
     */
    private fun formatJson(json: String): String {
        // 简单的JSON格式化（添加缩进）
        var indent = 0
        val indentSize = 2
        val result = StringBuilder()
        var inString = false
        var escapeNext = false
        
        for (char in json) {
            when {
                escapeNext -> {
                    result.append(char)
                    escapeNext = false
                }
                char == '\\' -> {
                    result.append(char)
                    escapeNext = true
                }
                char == '"' -> {
                    result.append(char)
                    inString = !inString
                }
                !inString && (char == '{' || char == '[') -> {
                    result.append(char).append('\n')
                    indent++
                    result.append(" ".repeat(indent * indentSize))
                }
                !inString && (char == '}' || char == ']') -> {
                    result.append('\n')
                    indent--
                    result.append(" ".repeat(indent * indentSize))
                    result.append(char)
                }
                !inString && char == ',' -> {
                    result.append(char).append('\n')
                    result.append(" ".repeat(indent * indentSize))
                }
                !inString && char == ':' -> {
                    result.append(char).append(' ')
                }
                else -> {
                    result.append(char)
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * 记录请求体或响应体（支持多行和截断）
     */
    private fun logBody(body: String) {
        val lines = body.lines()
        val maxLines = 50 // 最多显示50行
        
        if (lines.size <= maxLines) {
            lines.forEach { line ->
                // 如果单行过长，自动换行显示
                logLongLine(line)
            }
        } else {
            // 只显示前 maxLines 行
            lines.take(maxLines).forEach { line ->
                logLongLine(line)
            }
            Timber.tag("Network").d("│   ... [省略 ${lines.size - maxLines} 行]")
        }
    }
    
    /**
     * 记录长行（自动换行显示）
     */
    private fun logLongLine(line: String, maxLineLength: Int = 120, prefix: String = "│   ") {
        if (line.length <= maxLineLength) {
            Timber.tag("Network").d("$prefix$line")
        } else {
            // 长行自动换行显示
            var start = 0
            var isFirstLine = true
            while (start < line.length) {
                val end = minOf(start + maxLineLength, line.length)
                val chunk = line.substring(start, end)
                val continuation = if (end < line.length) " \\" else ""
                val linePrefix = if (isFirstLine) prefix else "│${" ".repeat(prefix.length - 2)}"
                Timber.tag("Network").d("$linePrefix$chunk$continuation")
                start = end
                isFirstLine = false
            }
        }
    }
    
    /**
     * 生成并打印 curl 命令
     */
    private fun logCurlCommand(request: okhttp3.Request) {
        try {
            val curlCommand = buildCurlCommand(request)
            val fullCommand = "curl $curlCommand"
            Timber.tag("Network").d("│")
            Timber.tag("Network").d("│ Curl Command:")
            logLongLine(fullCommand, maxLineLength = 100, prefix = "│   ")
        } catch (e: Exception) {
            Timber.tag("Network").d("│   [无法生成 curl 命令: ${e.message}]")
        }
    }
    
    /**
     * 构建 curl 命令
     */
    private fun buildCurlCommand(request: okhttp3.Request): String {
        val builder = StringBuilder()
        
        // 方法
        if (request.method != "GET") {
            builder.append("-X ${request.method} ")
        }
        
        // URL
        builder.append("'${request.url}' ")
        
        // Headers
        request.headers.forEach { header ->
            val value = if (isSensitiveHeader(header.first)) {
                "***"
            } else {
                // 转义单引号
                header.second.replace("'", "'\\''")
            }
            builder.append("-H '${header.first}: $value' ")
        }
        
        // Body
        if (request.body != null) {
            try {
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val requestBody = buffer.readUtf8()
                
                // 转义单引号和特殊字符
                val escapedBody = requestBody
                    .replace("'", "'\\''")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                
                builder.append("-d '$escapedBody' ")
            } catch (e: Exception) {
                // 如果无法读取请求体，跳过
            }
        }
        
        return builder.toString().trim()
    }
    
    /**
     * 判断是否为敏感请求头
     */
    private fun isSensitiveHeader(headerName: String): Boolean {
        val sensitiveHeaders = setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
            "x-access-token"
        )
        return sensitiveHeaders.contains(headerName.lowercase())
    }
}
