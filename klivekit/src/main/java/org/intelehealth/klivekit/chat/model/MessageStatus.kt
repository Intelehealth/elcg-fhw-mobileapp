package org.intelehealth.klivekit.chat.model


/**
 * Created by Vaghela Mithun R. on 16-03-2023 - 20:52.
 * Email : vaghela@codeglo.com
 * Mob   : +919727206702
 **/
enum class MessageStatus(val value: Int) {

    RECEIVED(3),
    DELIVERED(2),
    READ(1),
    CMD_RECEIVED(4),
    SENT(0),
    SENDING(-1),
    FAILED(-2);

    fun isReceived() = this == RECEIVED

    fun isDelivered() = this == DELIVERED

    fun isRead() = this == READ

    fun isCmdReceived() = this == CMD_RECEIVED

    fun isSent() = this == SENT

    fun isSending() = SENDING

    fun isFailed() = this == FAILED

    companion object {
        @JvmStatic
        fun getStatus(value: Int) = when (value) {
            READ.value -> READ
            SENDING.value -> SENDING
            SENT.value -> SENT
            DELIVERED.value -> DELIVERED
            FAILED.value -> FAILED
            else -> SENT
        }
    }
}