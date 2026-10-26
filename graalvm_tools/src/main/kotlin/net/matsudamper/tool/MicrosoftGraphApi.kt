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
        conflictBehavior: String? = null,
    ) {
        val session = run {
            val url = "https://graph.microsoft.com/v1.0/users/$userObjectId/drive/root:$filePath:/createUploadSession"

            val result = HttpClient.newHttpClient()
                .send(
                    HttpRequest.newBuilder(URI(url))
                        .header("Authorization", "Bearer $bearerToken")
                        .let { builder ->
                            if (conflictBehavior == null) {
                                builder.method("POST", HttpRequest.BodyPublishers.noBody())
                            } else {
                                builder.header("Content-Type", "application/json")
                                    .method(
                                        "POST", HttpRequest.BodyPublishers.ofString(
                                            """{"item":{"@microsoft.graph.conflictBehavior":"$conflictBehavior"}}"""
                                        )
                                    )
                            }
                        }
                        // HTTP2だとContent-Lengthが0の時に設定されず、API側でエラーになる
                        .version(HttpClient.Version.HTTP_1_1)
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
            val range = nextExpectedRanges.first()
            val startRange: Long
            val endRange: Long
            run {
                // https://learn.microsoft.com/en-us/graph/api/driveitem-createuploadsession?view=graph-rest-1.0#upload-bytes-to-the-upload-session
                val maxChunkSize = 60 * 1024 * 1024
                val list = range.split("-")
                startRange = list[0].toLong()
                endRange = run {
                    val tmp = list.getOrNull(1)
                        ?.takeIf { it.isNotBlank() }
                        ?.toLong()
                        ?: (startRange + maxChunkSize)

                    tmp.coerceAtMost(allSize - 1)
                        .coerceAtMost(startRange + maxChunkSize)
                }
            }

            val len = (endRange - startRange) + 1
            val byteArray = ByteArray(len.toInt()).also { byteArray ->
                inputStream().use { stream ->
                    stream.skipNBytes(startRange)
                    stream.read(byteArray)
                }
            }
            val contentRange = "bytes $startRange-$endRange/${allSize}"
            println("Content-Range: $contentRange")
            val result = HttpClient.newHttpClient()
                .send(
                    HttpRequest.newBuilder(URI(session.uploadUrl))
                        .header("Authorization", "Bearer $bearerToken")
                        .header("Content-Range", contentRange)
                        .method(
                            "PUT", HttpRequest.BodyPublishers.ofByteArray(byteArray)
                        )
                        .version(HttpClient.Version.HTTP_1_1)
                        .build(),
                    HttpResponse.BodyHandlers.ofString()
                )

            val statusCode = result.statusCode()
            println("upload: $statusCode")
            println(result.body())
            if (statusCode == 200 || statusCode == 201) {
                // アップロード完了(既存ファイルの上書き時は200)
                break
            }
            nextExpectedRanges = json.decodeFromString<MicrosoftGraphApiSessionUploadResponse>(result.body())
                .nextExpectedRanges

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
        if (result.statusCode() !in 200..299) {
            throw IllegalStateException("Failed to upload: ${result.statusCode()}")
        }
    }

    /**
     * フォルダ配下のファイルを再帰的に取得する。フォルダが存在しない場合は空を返す。
     * @param folderPath エンコード済みのパス
     * @return フォルダからの相対パスをキーにしたMap
     */
    fun listFilesRecursive(
        folderPath: String,
        bearerToken: String,
    ): Map<String, MicrosoftGraphApiDriveItem> {
        val result = mutableMapOf<String, MicrosoftGraphApiDriveItem>()
        fun traverse(encodedPath: String, relativePrefix: String) {
            val item = if (encodedPath.isEmpty()) "root" else "root:$encodedPath:"
            var url: String? = "https://graph.microsoft.com/v1.0/users/$userObjectId/drive/$item/children" +
                    "?\$select=name,size,file,folder&\$top=1000"
            while (url != null) {
                val response = HttpClient.newHttpClient()
                    .send(
                        HttpRequest.newBuilder(URI(url))
                            .header("Authorization", "Bearer $bearerToken")
                            .GET()
                            .version(HttpClient.Version.HTTP_1_1)
                            .build(),
                        HttpResponse.BodyHandlers.ofString()
                    )
                if (response.statusCode() == 404) return
                if (response.statusCode() != 200) {
                    throw IllegalStateException("Failed to list children: ${response.statusCode()} ${response.body()}")
                }
                val children = json.decodeFromString<MicrosoftGraphApiChildrenResponse>(response.body())
                for (item in children.value) {
                    val relativePath = relativePrefix + item.name
                    if (item.folder != null) {
                        traverse("$encodedPath/${encodePathSegment(item.name)}", "$relativePath/")
                    } else {
                        result[relativePath] = item
                    }
                }
                url = children.nextLink
            }
        }
        traverse(folderPath, "")
        return result
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

fun encodePathSegment(segment: String): String {
    return URLEncoder.encode(segment, Charsets.UTF_8).replace("+", "%20")
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


@Serializable
data class MicrosoftGraphApiChildrenResponse(
    @SerialName("value") val value: List<MicrosoftGraphApiDriveItem>,
    @SerialName("@odata.nextLink") val nextLink: String? = null,
)

@Serializable
data class MicrosoftGraphApiDriveItem(
    @SerialName("name") val name: String,
    @SerialName("size") val size: Long = 0,
    @SerialName("file") val file: File? = null,
    @SerialName("folder") val folder: JsonObject? = null,
) {
    @Serializable
    data class File(
        @SerialName("hashes") val hashes: Hashes? = null,
    )

    @Serializable
    data class Hashes(
        @SerialName("quickXorHash") val quickXorHash: String? = null,
    )
}
