package net.matsudamper.tool

import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import net.matsudamper.tool.json.IsoDateTimeInstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MicrosoftGraphApi(
    private val tenantId: String,
    private val clientId: String,
    private val clientSecret: String,
    private val userObjectId: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun createUploadSession(
        filePath: String,
        bearerToken: String,
        inputStream: () -> InputStream,
    ) {
        val session = run {
            val url = "https://graph.microsoft.com/v1.0/users/$userObjectId/drive/root:$filePath:/createUploadSession"

            val result = HttpClient.newHttpClient()
                .send(
                    HttpRequest.newBuilder(URI(url))
                        .header("Authorization", "Bearer $bearerToken")
                        .method(
                            "POST", HttpRequest.BodyPublishers.noBody()
                        )
                        .version(HttpClient.Version.HTTP_1_1) // HTTP2だとContent-Lengthが0の時に設定されず、API側でエラーになる
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                )

            val body = result.body()
            println("CreateUploadSession: ${result.statusCode()}")
            runCatching {
                json.decodeFromString(
                    MicrosoftGraphApiCreateSessionResponse.serializer(),
                    body
                )
            }.onFailure {
                println("Failed to decode: $body")
            }.getOrThrow()
        }

        var nextExpectedRanges: List<String> = session.nextExpectedRanges

        val allSize = inputStream().size()
        while (nextExpectedRanges.isNotEmpty()) {
            nextExpectedRanges.forEach {
                val startRange: Long
                val endRange: Long
                run {
                    val list = it.split("-")
                    startRange = list[0].toLong()
                    endRange = run {
                        val tmp = list.getOrNull(1)
                            ?.takeIf { it.isNotBlank() }
                            ?.toLong()
                            ?: (startRange + 1000)

                        tmp.coerceAtMost(allSize)
                    }
                }

                val len = (endRange - startRange) + 1
                val byteArray = ByteArray(len.toInt()).also { byteArray ->
                    inputStream().use { stream ->
                        stream.skipNBytes(startRange)
                        stream.read(byteArray)
                    }
                }

                val result = HttpClient.newHttpClient()
                    .send(
                        HttpRequest.newBuilder(URI(session.uploadUrl))
                            .header("Authorization", "Bearer $bearerToken")
                            .header("Content-Range", "bytes $startRange-$endRange/${allSize}")
                            .method(
                                "PUT", HttpRequest.BodyPublishers.ofByteArray(byteArray)
                            )
                            .version(HttpClient.Version.HTTP_1_1)
                            .build(),
                        HttpResponse.BodyHandlers.ofString()
                    )

                println("upload: ${result.statusCode()}")
                println(result.body())
                nextExpectedRanges = json.decodeFromString<MicrosoftGraphApiSessionUploadResponse>(result.body())
                    .nextExpectedRanges
            }
        }
    }

    fun upload(
        filePath: String,
        bearerToken: String,
        inputStream: InputStream,
    ) {
        val url = "https://graph.microsoft.com/v1.0/users/$userObjectId/drive/root:$filePath:/content"

        val result = HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI(url))
                    .header("Authorization", "Bearer $bearerToken")
                    .header("Content-Type", "text/plain")
                    .method(
                        "PUT", HttpRequest.BodyPublishers.ofInputStream {
                            inputStream
                        }
                    )
                    .version(HttpClient.Version.HTTP_1_1)
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )

        println(result.statusCode())
        println(result.body())
    }

    fun getBearerToken(): String {
        val result = HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI("https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .method(
                        "POST", HttpRequest.BodyPublishers.ofString(
                            createFormParam(
                                "client_id" to clientId,
                                "scope" to "https://graph.microsoft.com/.default",
                                "client_secret" to clientSecret,
                                "grant_type" to "client_credentials",
                            )
                        )
                    )
                    .version(HttpClient.Version.HTTP_1_1)
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
        val response = result.body()
        println("Get BearerToken: ${result.statusCode()}")

        return Json.decodeFromString<JsonObject>(response)
            .getValue("access_token")
            .jsonPrimitive
            .content
    }

    private fun createFormParam(vararg keyValue: Pair<String, String>): String {
        return keyValue.map {
            URLEncoder.encode(it.first, Charsets.UTF_8) to
                    URLEncoder.encode(it.second, Charsets.UTF_8)
        }.joinToString("&") { "${it.first}=${it.second}" }
    }
}

private fun InputStream.size(): Long {
    var size: Long = 0
    do {
        val availableSize = available()
        size += availableSize
        skipNBytes(availableSize.toLong())
    } while (availableSize != 0)
    return size
}

@Serializable
data class MicrosoftGraphApiCreateSessionResponse(
    @SerialName("uploadUrl") val uploadUrl: String,
    @Serializable(IsoDateTimeInstantSerializer::class)
    @SerialName("expirationDateTime") val expirationDateTime: Instant,
    @SerialName("nextExpectedRanges") val nextExpectedRanges: List<String>,
)


@Serializable
data class MicrosoftGraphApiSessionUploadResponse(
    @Serializable(IsoDateTimeInstantSerializer::class)
    @SerialName("expirationDateTime") val expirationDateTime: Instant,
    @SerialName("nextExpectedRanges") val nextExpectedRanges: List<String>,
)

