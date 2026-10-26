package net.matsudamper.tool

import command.Command
import command.CommandExecutor
import command.ExecutableCommand
import command.NestedHostCommand
import java.io.File
import kotlin.system.exitProcess

val microsoftGraphApi by lazy {
    MicrosoftGraphApi(
        tenantId = System.getenv("tenantId"),
        clientId = System.getenv("clientId"),
        clientSecret = System.getenv("clientSecret"),
        userObjectId = System.getenv("userObjectId"),
    )
}

fun main(args: Array<String>) {
    CommandExecutor(
        listOf(
            object : NestedHostCommand {
                override val names: List<String> = listOf("sharepoint", "onedrive")
                override val help: String = "SharePoint(OneDrive)"
                override val subCommands: List<Command> = listOf(
                    object : ExecutableCommand {
                        override val names: List<String> = listOf("upload")
                        override val help: String = "upload <Local FileName> <Remote FileName>"
                        override fun exec(arguments: List<String>) {
                            if (arguments.size != 2) {
                                System.err.println("Illegal argument size.")
                                System.err.println(help)
                                exitProcess(1)
                            }
                            val local = arguments[0]
                            val remote = arguments[1]

                            val token = microsoftGraphApi.getBearerToken()
                            microsoftGraphApi.createUploadSession(
                                filePath = remote,
                                bearerToken = token,
                                inputStream = { File(local).inputStream() },
                            )
                        }
                    },
                    object : ExecutableCommand {
                        override val names: List<String> = listOf("upload-diff")
                        override val help: String = "upload-diff [--dry-run] <Local Directory> <Remote Directory>"
                        override fun exec(arguments: List<String>) {
                            val dryRun = arguments.contains("--dry-run")
                            val paths = arguments.filterNot { it == "--dry-run" }
                            if (paths.size != 2) {
                                System.err.println("Illegal argument size.")
                                System.err.println(help)
                                exitProcess(1)
                            }
                            val localDir = File(paths[0])
                            if (localDir.isDirectory.not()) {
                                System.err.println("Not a directory: $localDir")
                                exitProcess(1)
                            }
                            val remoteDir = paths[1].split("/")
                                .filter { it.isNotEmpty() }
                                .joinToString("") { "/" + encodePathSegment(it) }

                            val token = microsoftGraphApi.getBearerToken()
                            val remoteFiles = microsoftGraphApi.listFilesRecursive(
                                folderPath = remoteDir,
                                bearerToken = token,
                            )

                            val localFiles = localDir.walkTopDown()
                                .filter { it.isFile }
                                .sortedBy { it.path }
                                .toList()
                            var skippedCount = 0
                            var uploadedCount = 0
                            var uploadedSize = 0L
                            for (file in localFiles) {
                                val relativePath = file.relativeTo(localDir).invariantSeparatorsPath
                                val remoteFile = remoteFiles[relativePath]
                                val label = when {
                                    remoteFile == null -> "+ added  "
                                    remoteFile.size != file.length() -> "~ updated"
                                    remoteFile.file?.hashes?.quickXorHash != QuickXorHash.hashBase64(file.inputStream()) -> "~ updated"
                                    else -> null
                                }
                                if (label == null) {
                                    skippedCount++
                                    continue
                                }
                                println("$label $relativePath")
                                if (dryRun.not()) {
                                    val remotePath = remoteDir + relativePath.split("/")
                                        .joinToString("") { "/" + encodePathSegment(it) }
                                    if (file.length() == 0L) {
                                        // アップロードセッションは空ファイルに対応していない
                                        microsoftGraphApi.upload(
                                            filePath = remotePath,
                                            bearerToken = token,
                                            inputStream = file.inputStream(),
                                        )
                                    } else {
                                        microsoftGraphApi.createUploadSession(
                                            filePath = remotePath,
                                            bearerToken = token,
                                            inputStream = { file.inputStream() },
                                            conflictBehavior = "replace",
                                        )
                                    }
                                }
                                uploadedCount++
                                uploadedSize += file.length()
                            }
                            println("= skipped  $skippedCount files")
                            val verb = if (dryRun) "Would upload" else "Uploaded"
                            println("$verb $uploadedCount files ($uploadedSize bytes)")
                        }
                    },
                )
            }
        )
    ).exec(args.toList())
}
