package kst4contest.view.compose

import kst4contest.model.ChatMessage

/**
 * The columns of the selected station message tab.
 */
object SelectedStationMessageColumns {

    fun all(): List<DataColumn<ChatMessage>> = listOf(
        DataColumn(
            id = "time",
            title = "Time",
            value = { MessageFormats.clockTime(it.messageGeneratedTime) },
            weight = 0.7f,
        ),
        DataColumn(
            id = "call-tx",
            title = "Sender",
            value = { MonitorColumns.callSignOf(it.sender) },
            weight = 0.8f,
        ),
        DataColumn(
            id = "call-rx",
            title = "Receiver",
            value = { MonitorColumns.callSignOf(it.receiver) },
            weight = 0.8f,
        ),
        DataColumn(
            id = "qrg-tx",
            title = "Last QRG TX",
            value = { it.sender?.frequency?.get()?.takeIf { s -> s.isNotBlank() } ?: "" },
            weight = 0.8f,
        ),
        DataColumn(
            id = "qrg-rx",
            title = "Last QRG RX",
            value = { it.receiver?.frequency?.get()?.takeIf { s -> s.isNotBlank() } ?: "" },
            weight = 0.8f,
        ),
        DataColumn(
            id = "message",
            title = "Message",
            value = { it.messageText.orEmpty() },
            weight = 3f,
        ),
        DataColumn(
            id = "wkd-rx",
            title = "wkd RX?",
            value = { if (it.receiver?.isWorked == true) "wkd" else "" },
            weight = 0.6f,
        ),
        DataColumn(
            id = "wkd-tx",
            title = "wkd TX?",
            value = { if (it.sender?.isWorked == true) "wkd" else "" },
            weight = 0.6f,
        ),
    )
}
