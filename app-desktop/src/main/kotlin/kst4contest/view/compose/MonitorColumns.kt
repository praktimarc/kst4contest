package kst4contest.view.compose

import kst4contest.controller.Utils4KST
import kst4contest.model.ChatMember
import kst4contest.model.ChatMessage
import kst4contest.model.ClusterMessage

/**
 * The columns of the two monitor tables.
 *
 * The ids are not free to choose: they are part of the operator's stored layout, so
 * renaming one loses their column widths without saying so. They are kept exactly as
 * `TableLayoutManager.column(...)` registered them.
 *
 * Every value survives a half-filled message. These tables are fed straight from the
 * wire, a sender or receiver can be missing, and a NullPointerException in a live
 * table during a contest is the worst possible moment for one.
 */
object MonitorColumns {

    fun dxCluster(): List<DataColumn<ClusterMessage>> = listOf(
        DataColumn(id = "time", title = { kst4contest.view.i18n.CurrentStrings.get().columnTime }, value = { MessageFormats.clockTime(it.timeGenerated) }, weight = 0.7f),
        DataColumn(id = "call-tx", title = "Call tx", value = { callSignOf(it.sender) }),
        DataColumn(id = "locator-tx", title = "LOC tx", value = { qraOf(it.sender) }, weight = 0.7f),
        DataColumn(id = "call-rx", title = "Call rx", value = { callSignOf(it.receiver) }),
        DataColumn(id = "locator-rx", title = "LOC rx", value = { qraOf(it.receiver) }, weight = 0.7f),
        DataColumn(id = "qrg", title = "QRG", value = { MessageFormats.paddedQrg(frequencyOf(it.receiver)) }, weight = 0.8f),
        DataColumn(id = "message", title = { kst4contest.view.i18n.CurrentStrings.get().columnMessage }, value = { it.messageInhibited.orEmpty() }, weight = 2.5f),
        /* "X" and not "true": the JavaFX cell printed a cross. */
        DataColumn(id = "worked", title = "wkd", value = { if (it.isReceiverWkd) "X" else "" }, weight = 0.4f),
    )

    /**
     * Nine columns, not ten. The JavaFX method built a "Name" column and never added
     * it to the table; porting it would put a column on screen that release 1.49
     * never showed.
     */
    fun qsoOfTheOther(): List<DataColumn<ChatMessage>> = listOf(
        DataColumn(id = "time", title = { kst4contest.view.i18n.CurrentStrings.get().columnTime }, value = { MessageFormats.clockTime(it.messageGeneratedTime) }, weight = 0.7f),
        DataColumn(id = "call-tx", title = "Call TX", value = { callSignOf(it.sender) }),
        DataColumn(id = "last-qrg-tx", title = { kst4contest.view.i18n.CurrentStrings.get().columnLastQrgTx }, value = { frequencyOf(it.sender) }, weight = 0.8f),
        DataColumn(id = "worked-tx", title = "wkd TX?", value = { workedMark(it.sender) }, weight = 0.4f),
        DataColumn(id = "call-rx", title = "Call RX", value = { callSignOf(it.receiver) }),
        DataColumn(id = "last-qrg-rx", title = { kst4contest.view.i18n.CurrentStrings.get().columnLastQrgRx }, value = { frequencyOf(it.receiver) }, weight = 0.8f),
        DataColumn(id = "worked-rx", title = "wkd RX?", value = { workedMark(it.receiver) }, weight = 0.4f),
        DataColumn(id = "message", title = { kst4contest.view.i18n.CurrentStrings.get().columnMessage }, value = { it.messageText.orEmpty() }, weight = 2.5f),
        /*
         * From the sender, not from the message. Both carry a field of that name and
         * they are set on different parsing paths, so they are not always the same
         * value; the JavaFX cell read the sender's.
         */
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

    internal fun callSignOf(member: ChatMember?): String = member?.callSign.orEmpty()

    internal fun qraOf(member: ChatMember?): String = member?.qra.orEmpty()

    internal fun frequencyOf(member: ChatMember?): String = member?.frequency?.get().orEmpty()

    private fun workedMark(member: ChatMember?): String =
        if (member != null && member.isWorked) "X" else ""
}
