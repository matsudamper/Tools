import java.io.File

val microsoftGraphApi by lazy {
    MicrosoftGraphApi(
        tenantId = System.getenv("tenantId"),
        clientId = System.getenv("clientId"),
        clientSecret = System.getenv("clientSecret"),
        userObjectId = System.getenv("userObjectId"),
    )
}
