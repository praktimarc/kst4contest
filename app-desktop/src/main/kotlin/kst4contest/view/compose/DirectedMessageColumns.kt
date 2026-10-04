package kst4contest.view.compose

import kst4contest.model.ChatMessage

/**
 * The columns of the directed-message table: what was said to this station.
 *
 * Its own pane above the send line rather than one of the tabs, because an operator must
 * be able to see a private call without switching away from whatever they are reading.
 *
 * The ids are the keys the operator's stored column widths hang on, exactly as
 * TableLayoutManager registered them under "private-messages".
 *
 * @param ownCallSign the local station, which gets no distance to itself
 * @param formatMessage the controller's display formatting for a message text
 */
object DirectedMessageColumns {

    fun all(
        ownCallSign: () -> String? = { null },
        formatMessage: (ChatMessage) -> String,
    ): List<DataColumn<ChatMessage>> = listOf(
        DataColumn(
            id = "time",
            title = "Time",
            value = { MessageFormats.clockTime(it.messageGeneratedTime) },
            weight = 0.7f,
        ),
        DataColumn(
            id = "callsign",
            title = "Callsign",
            value = { MonitorColumns.callSignOf(it.sender) },
        ),
        DataColumn(id = "name", title = "Name", value = { it.sender?.name.orEmpty() }),
        DataColumn(id = "qra", title = "QRA", value = { MonitorColumns.qraOf(it.sender) }),
        DataColumn(
            id = "qrb",
            title = "QRB",
            value = { message -> qrbOf(message, ownCallSign()) },
            comparator = compareBy { it.sender?.qrb ?: 0.0 },
            weight = 0.9f,
        ),
        DataColumn(
            id = "message",
            title = "Message",
            value = formatMessage,
            weight = 3f,
        ),
        DataColumn(
            id = "last-qrg",
            title = "Last known QRG",
            value = { MessageFormats.paddedQrg(MonitorColumns.frequencyOf(it.sender)) },
            weight = 0.9f,
        ),
        DataColumn(
            id = "airscout",
            title = "AP [minutes / pot%]",
            value = { AirplaneCell.format(it.sender?.airPlaneReflectInfo) },
            accent = { _, rendered -> AirplaneCell.accentFor(rendered) },
            weight = 1.4f,
        ),
        DataColumn(
            id = "category",
            title = "Category",
            value = { message ->
                message.chatCategory
                    ?.let { it.getChatCategoryName(it.categoryNumber) }
                    .orEmpty()
            },
            weight = 1.2f,
        ),
    )

    /**
     * Distance and bearing as one cell, both cut to whole numbers to save width.
     *
     * The degree sign sits outside the bracket — "342 km (88)°" — which is how JavaFX
     * wrote it and therefore how operators read it.
     */
    private fun qrbOf(message: ChatMessage, ownCallSign: String?): String {
        val sender = message.sender ?: return ""
        if (sender.callSign == null || sender.callSign == ownCallSign) return ""

        val qrb = sender.qrb ?: return ""
        val bearing = sender.getQTFdirection() ?: return ""
        return "${qrb.toInt()} km (${bearing.toInt()})°"
    }
}
