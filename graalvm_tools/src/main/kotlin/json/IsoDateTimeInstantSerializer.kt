package json

import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.jvm.jvmName

object IsoDateTimeInstantSerializer : KSerializer<Instant> {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        IsoDateTimeInstantSerializer::class.jvmName,
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): Instant {
        val string = decoder.decodeString()
        return Instant.from(formatter.parse(string))
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }
}
