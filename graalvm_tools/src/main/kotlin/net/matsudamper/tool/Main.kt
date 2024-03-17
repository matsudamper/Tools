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
                )
            }
        )
    ).exec(args.toList())
}
