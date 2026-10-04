package kst4contest.view.compose

import kst4contest.model.ChatMessage

/**
 * The columns of the public message tab.
 *
 * The other two tabs below the station list reuse the monitor tables under their own
 * layout ids; this one is its own. Its ids are the operator's stored column widths,
 * kept exactly as TableLayoutManager registered them.
 */
object PublicMessageColumns {

    fun all(): List<DataColumn<ChatMessage>> = listOf(
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
        DataColumn(
            id = "last-qrg",
            title = "Last QRG",
            value = { MessageFormats.paddedQrg(MonitorColumns.frequencyOf(it.sender)) },
            weight = 0.8f,
        ),
        DataColumn(
            id = "message",
            title = "Message",
            value = { it.messageText.orEmpty() },
            weight = 3f,
        ),
        DataColumn(
            id = "category",
            title = "Category",
            value = { message ->
                message.sender?.chatCategory
                    ?.let { it.getChatCategoryName(it.categoryNumber) }
                    .orEmpty()
            },
            weight = 1.2f,
        ),
    )
}
