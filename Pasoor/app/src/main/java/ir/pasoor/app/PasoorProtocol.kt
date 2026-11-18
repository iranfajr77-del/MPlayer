package ir.pasoor.app

import org.json.JSONArray
import org.json.JSONObject

/**
 * Versioned, newline-safe message contract for the offline Bluetooth table.
 * The host is authoritative: clients send intents; only host snapshots update a game.
 */
object PasoorProtocol {
    const val VERSION = 1

    sealed class Message {
        data class Hello(val playerName: String, val requestedSeat: Int?) : Message()
        data class Welcome(val playerId: String, val seat: Int, val host: Boolean) : Message()
        data class SeatRequest(val seat: Int) : Message()
        data class SeatUpdate(val seats: List<Seat>) : Message()
        data class TrumpRequest(val suit: Suit) : Message()
        data class PlayRequest(val card: Card) : Message()
        data class GameSnapshot(val state: MatchState) : Message()
        data class Notice(val text: String) : Message()
    }

    data class Seat(val index: Int, val playerId: String?, val playerName: String, val isBot: Boolean)

    fun encode(message: Message): String {
        val root = JSONObject().put("v", VERSION)
        when (message) {
            is Message.Hello -> root.put("type", "hello").put("name", message.playerName)
                .put("requestedSeat", message.requestedSeat ?: JSONObject.NULL)
            is Message.Welcome -> root.put("type", "welcome").put("playerId", message.playerId)
                .put("seat", message.seat).put("host", message.host)
            is Message.SeatRequest -> root.put("type", "seat_request").put("seat", message.seat)
            is Message.SeatUpdate -> {
                root.put("type", "seats")
                val array = JSONArray()
                message.seats.forEach { seat ->
                    array.put(JSONObject().put("index", seat.index).put("playerId", seat.playerId ?: JSONObject.NULL)
                        .put("name", seat.playerName).put("bot", seat.isBot))
                }
                root.put("seats", array)
            }
            is Message.TrumpRequest -> root.put("type", "trump_request").put("suit", message.suit.name)
            is Message.PlayRequest -> root.put("type", "play_request").put("card", cardJson(message.card))
            is Message.GameSnapshot -> root.put("type", "snapshot").put("state", stateJson(message.state))
            is Message.Notice -> root.put("type", "notice").put("text", message.text)
        }
        return root.toString()
    }

    fun decode(raw: String): Message {
        val root = JSONObject(raw)
        require(root.optInt("v", -1) == VERSION) { "نسخه پیام بلوتوث سازگار نیست" }
        return when (root.getString("type")) {
            "hello" -> Message.Hello(root.optString("name", "بازیکن"),
                if (root.isNull("requestedSeat")) null else root.getInt("requestedSeat").also { require(it in 1..3) })
            "welcome" -> Message.Welcome(root.getString("playerId"), root.getInt("seat"), root.getBoolean("host"))
            "seat_request" -> Message.SeatRequest(root.getInt("seat").also { require(it in 1..3) })
            "seats" -> {
                val array = root.getJSONArray("seats")
                Message.SeatUpdate((0 until array.length()).map { i ->
                    val x = array.getJSONObject(i)
                    Seat(x.getInt("index"), if (x.isNull("playerId")) null else x.getString("playerId"),
                        x.getString("name"), x.getBoolean("bot"))
                })
            }
            "trump_request" -> Message.TrumpRequest(Suit.valueOf(root.getString("suit")))
            "play_request" -> Message.PlayRequest(readCard(root.getJSONObject("card")))
            "snapshot" -> Message.GameSnapshot(readState(root.getJSONObject("state")))
            "notice" -> Message.Notice(root.optString("text"))
            else -> throw IllegalArgumentException("پیام ناشناخته از دستگاه دیگر")
        }
    }

    private fun cardJson(card: Card) = JSONObject().put("suit", card.suit.name).put("rank", card.rank)
    private fun readCard(json: JSONObject) = Card(Suit.valueOf(json.getString("suit")), json.getInt("rank").also { require(it in 2..14) })

    private fun stateJson(s: MatchState): JSONObject {
        val hands = JSONArray()
        s.hands.forEach { hand -> val a = JSONArray(); hand.forEach { a.put(cardJson(it)) }; hands.put(a) }
        val trick = JSONArray()
        s.trick.forEach { trick.put(JSONObject().put("seat", it.seat).put("card", cardJson(it.card))) }
        return JSONObject().put("hands", hands).put("trump", s.trump?.name ?: JSONObject.NULL)
            .put("leader", s.leader).put("turn", s.turn).put("trick", trick)
            .put("tricks", intArray(s.tricks)).put("points", intArray(s.points))
            .put("handNo", s.handNo).put("phase", s.phase).put("message", s.message)
            .put("stock", JSONArray().also { a -> s.stock.forEach { a.put(cardJson(it)) } })
    }

    private fun readState(j: JSONObject): MatchState {
        val handsJson = j.getJSONArray("hands")
        require(handsJson.length() == 4)
        val hands = (0 until 4).map { i ->
            val a = handsJson.getJSONArray(i)
            (0 until a.length()).map { readCard(a.getJSONObject(it)) }
        }
        val t = j.getJSONArray("trick")
        val trick = (0 until t.length()).map { i ->
            val x = t.getJSONObject(i)
            Played(x.getInt("seat").also { require(it in 0..3) }, readCard(x.getJSONObject("card")))
        }
        val tricks = readInts(j.getJSONArray("tricks"), 4)
        val points = readInts(j.getJSONArray("points"), 2)
        val stockJson = j.getJSONArray("stock")
        val stock = (0 until stockJson.length()).map { readCard(stockJson.getJSONObject(it)) }
        val trump = if (j.isNull("trump")) null else Suit.valueOf(j.getString("trump"))
        return MatchState(hands, trump, j.getInt("leader"), j.getInt("turn"), trick, tricks, points,
            j.getInt("handNo"), j.getString("phase"), j.getString("message"), stock)
    }

    private fun intArray(values: List<Int>) = JSONArray().also { a -> values.forEach(a::put) }
    private fun readInts(a: JSONArray, expected: Int): List<Int> {
        require(a.length() == expected)
        return (0 until a.length()).map { a.getInt(it) }
    }
}
