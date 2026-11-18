package ir.pasoor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF10100F)
private val Gold = Color(0xFFD6AD55)
private val Cream = Color(0xFFF4E8D2)
private val Green = Color(0xFF164B36)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PasoorApp() }
    }
}

enum class Suit(val symbol: String, val fa: String, val red: Boolean) {
    HEARTS("♥", "دل", true), DIAMONDS("♦", "خشت", true),
    CLUBS("♣", "گشنیز", false), SPADES("♠", "پیک", false)
}
data class Card(val suit: Suit, val rank: Int) {
    val face: String get() = when (rank) { 11 -> "J"; 12 -> "Q"; 13 -> "K"; 14 -> "A"; else -> rank.toString() }
    val label: String get() = face + suit.symbol
}
data class Played(val seat: Int, val card: Card)
data class MatchState(
    val hands: List<List<Card>>, val trump: Suit?, val leader: Int, val turn: Int,
    val trick: List<Played>, val tricks: List<Int>, val points: List<Int>,
    val handNo: Int, val phase: String, val message: String, val stock: List<Card> = emptyList()
)

private fun newDeck(): List<Card> = Suit.values().flatMap { s -> (2..14).map { Card(s, it) } }.shuffled()
private fun team(seat: Int) = seat % 2
private fun legalCards(hand: List<Card>, trick: List<Played>): List<Card> {
    if (trick.isEmpty()) return hand
    val suit = trick.first().card.suit
    return hand.filter { it.suit == suit }.ifEmpty { hand }
}
private fun winner(trick: List<Played>, trump: Suit): Int {
    val lead = trick.first().card.suit
    return trick.maxWithOrNull(compareBy<Played> {
        when (it.card.suit) { trump -> 2; lead -> 1; else -> 0 }
    }.thenBy { it.card.rank })!!.seat
}
private fun botCard(hand: List<Card>, trick: List<Played>, trump: Suit): Card {
    val legal = legalCards(hand, trick)
    if (trick.isEmpty()) {
        val strongest = legal.groupBy { it.suit }.maxByOrNull { e -> e.value.sumOf { it.rank } + e.value.size * 3 }!!.key
        return legal.filter { it.suit == strongest }.minByOrNull { it.rank }!!
    }
    val lead = trick.first().card.suit
    val best = trick.maxWithOrNull(compareBy<Played> {
        when (it.card.suit) { trump -> 2; lead -> 1; else -> 0 }
    }.thenBy { it.card.rank })!!.card
    val canBeat = legal.filter { c ->
        val cValue = when (c.suit) { trump -> 2; lead -> 1; else -> 0 }
        val bValue = when (best.suit) { trump -> 2; lead -> 1; else -> 0 }
        cValue > bValue || (cValue == bValue && c.rank > best.rank)
    }
    return if (canBeat.isNotEmpty()) canBeat.minByOrNull { it.rank }!! else legal.minByOrNull { it.rank }!!
}
private fun deal(): Pair<List<List<Card>>, List<Card>> {
    val deck = newDeck()
    val firstFive = (0..3).map { deck.drop(it * 5).take(5) }
    return firstFive to deck.drop(20)
}
private fun finishDeal(s: MatchState, suit: Suit): MatchState {
    val fullHands = s.hands.mapIndexed { i, h -> h + s.stock.drop(i * 8).take(8) }
    return runBots(s.copy(hands=fullHands, stock=emptyList(), trump=suit, phase="play", message="حکم " + suit.fa + " است"))
}
private fun initialState(): MatchState {
    val d = deal()
    return MatchState(d.first, null, 0, 0, emptyList(), listOf(0,0,0,0), listOf(0,0), 1, "trump", "پنج کارت اول را ببین و حکم را انتخاب کن", d.second)
}
private fun chooseTrump(s: MatchState, suit: Suit): MatchState = finishDeal(s, suit)
private fun playCard(s: MatchState, card: Card): MatchState {
    if (s.turn != 0 || card !in legalCards(s.hands[0], s.trick)) return s
    val next = s.copy(hands=s.hands.mapIndexed { i,h -> if (i==0) h-card else h },
        trick=s.trick + Played(0,card), turn=1)
    return runBots(next)
}
private fun runBots(start: MatchState): MatchState {
    var s = start
    while (s.phase == "play" && s.trump != null && s.turn != 0) {
        val seat = s.turn
        val card = botCard(s.hands[seat], s.trick, s.trump)
        s = s.copy(hands=s.hands.mapIndexed { i,h -> if (i==seat) h-card else h },
            trick=s.trick + Played(seat,card), turn=(seat+1)%4)
        if (s.trick.size == 4) s = finishTrick(s)
    }
    if (s.trick.size == 4) s = finishTrick(s)
    return s
}
private fun finishTrick(s: MatchState): MatchState {
    val trump = s.trump ?: return s
    val seat = winner(s.trick, trump)
    val counts = s.tricks.mapIndexed { i,n -> if (i==seat) n+1 else n }
    val winningTeam = team(seat)
    val teamTricks = counts[winningTeam] + counts[winningTeam + 2]
    if (teamTricks >= 7) {
        val pts = s.points.mapIndexed { i,n -> if (i==winningTeam) n+1 else n }
        if (pts[winningTeam] >= 7) return s.copy(tricks=counts, points=pts, phase="done",
            message=if (winningTeam==0) "تبریک! تیم شما برنده شد 🏆" else "ربات‌ها برنده شدند")
        val next = nextHand(s.copy(tricks=counts, points=pts))
        return next.copy(message=if (winningTeam==0) "تیم شما این دور را برد" else "تیم ربات‌ها این دور را برد")
    }
    if (s.hands.all { it.isEmpty() }) return nextHand(s.copy(tricks=counts))
    return s.copy(tricks=counts, leader=seat, turn=seat, trick=emptyList(), message="دست برای جایگاه " + (seat+1))
}
private fun nextHand(old: MatchState): MatchState {
    val d = deal()
    val hakem = old.handNo % 4
    val base = old.copy(hands=d.first, stock=d.second, trump=null, leader=hakem, turn=hakem, trick=emptyList(),
        tricks=listOf(0,0,0,0), handNo=old.handNo+1,
        phase="trump",
        message=if (hakem==0) "نوبت حاکم شماست؛ حکم را انتخاب کن" else "حاکم: جایگاه " + (hakem+1))
    if (hakem==0) return base
    val suit = Suit.values().maxByOrNull { x -> d.first[hakem].filter { it.suit==x }.sumOf { it.rank } }!!
    return finishDeal(base, suit)
}

@Composable
fun PasoorApp() {
    var game by remember { mutableStateOf(initialState()) }
    var screen by remember { mutableStateOf("home") }
    var playerName by remember { mutableStateOf("بازیکن") }
    var botNames by remember { mutableStateOf(randomBotNames()) }
    MaterialTheme(colorScheme=darkColorScheme(primary=Gold, secondary=Gold, background=Ink, surface=Color(0xFF1B1916), onPrimary=Ink, onBackground=Cream, onSurface=Cream)) {
        Surface(Modifier.fillMaxSize(), color=Ink) {
            when (screen) {
                "home" -> Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                    Text("♠  ♥  ♣  ♦", color=Gold, fontSize=36.sp)
                    Text("پاسور", color=Gold, fontSize=48.sp, fontWeight=FontWeight.Black)
                    Text("حکم ایرانی • آفلاین", color=Cream.copy(alpha=.75f))
                    Spacer(Modifier.height(34.dp))
                    MenuButton("بازی با ربات‌ها") { game=initialState(); botNames=randomBotNames(); screen="game" }
                    MenuButton("بازی بلوتوثی") { screen="bluetooth" }
                    MenuButton("انتخاب نام بازیکن") { screen="name" }
                    Text("نسخه آزمایشی ۰٫۱", color=Gold.copy(alpha=.65f), modifier=Modifier.padding(top=24.dp))
                }
                "bluetooth" -> BluetoothLobby { screen="home" }
                "name" -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement=Arrangement.Center) {
                    Text("نام بازیکن", color=Gold, fontSize=28.sp)
                    OutlinedTextField(value=playerName, onValueChange={playerName=it}, label={Text("نام شما")})
                    MenuButton("ذخیره") { screen="home" }
                }
                else -> GameBoard(game, playerName, botNames, { game=chooseTrump(game,it) }, { game=playCard(game,it) }, { game=initialState() }, { screen="home" })
            }
        }
    }
}
@Composable
private fun MenuButton(label: String, enabled: Boolean=true, action: ()->Unit) {
    Button(onClick=action, enabled=enabled, modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Gold, contentColor=Ink, disabledContainerColor=Color.DarkGray)) {
        Text(label, fontWeight=FontWeight.Bold)
    }
}
@Composable
private fun GameBoard(game: MatchState, name: String, botNames: List<String>, onTrump:(Suit)->Unit, onPlay:(Card)->Unit, onRestart:()->Unit, onHome:()->Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) {
            Text("پاسور", color=Gold, fontSize=24.sp, fontWeight=FontWeight.Bold)
            Text(game.points[0].toString()+" - "+game.points[1].toString(), color=Cream, fontWeight=FontWeight.Bold)
        }
        Text("ربات بالا: "+game.hands[2].size+" کارت", color=Gold)
        Text("ربات چپ: "+game.hands[1].size+"  •  ربات راست: "+game.hands[3].size, color=Cream.copy(alpha=.7f), fontSize=12.sp)
        Box(Modifier.fillMaxWidth().weight(1f).padding(vertical=12.dp).background(Green, RoundedCornerShape(22.dp)).border(3.dp,Gold,RoundedCornerShape(22.dp)).padding(12.dp), contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("فرش ایرانی", color=Gold, fontWeight=FontWeight.Bold, fontSize=18.sp)
                Text(game.message, color=Cream, textAlign=TextAlign.Center, modifier=Modifier.padding(8.dp))
                if (game.trump!=null) Text("حکم: "+game.trump.symbol+" "+game.trump.fa, color=Gold, fontSize=22.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    game.trick.forEach { p -> Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(if (p.seat==0) name else botNames[p.seat], color=Cream, fontSize=10.sp)
                        CardFace(p.card)
                    } }
                }
                Text("دست‌ها: شما "+(game.tricks[0]+game.tricks[2])+"  |  ربات‌ها "+(game.tricks[1]+game.tricks[3]), color=Gold, fontSize=13.sp)
                if (game.phase=="trump") {
                    Spacer(Modifier.height(12.dp))
                    Text("خال حکم را انتخاب کن", color=Cream)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Suit.values().forEach { suit -> Button(onClick={onTrump(suit)}, colors=ButtonDefaults.buttonColors(containerColor=Cream,contentColor=if(suit.red) Color.Red else Ink)) { Text(suit.symbol) } }
                    }
                }
                if (game.phase=="done") {
                    Text(game.message, color=Gold, fontSize=20.sp, fontWeight=FontWeight.Bold)
                    MenuButton("شروع دوباره", action=onRestart)
                }
            }
        }
        Text("دست شما", color=Gold, fontWeight=FontWeight.Bold)
        LazyRow(horizontalArrangement=Arrangement.spacedBy(5.dp), contentPadding=PaddingValues(vertical=8.dp)) {
            items(game.hands[0].sortedWith(compareBy<Card>{it.suit.ordinal}.thenBy{it.rank})) { card ->
                val allowed=game.phase=="play" && game.turn==0 && card in legalCards(game.hands[0],game.trick)
                CardFace(card, allowed) { if(allowed) onPlay(card) }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick=onHome) { Text("خانه") }
            OutlinedButton(onClick=onRestart) { Text("شروع مجدد") }
        }
    }
}
@Composable
private fun CardFace(card:Card, active:Boolean=false, onClick:(()->Unit)?=null) {
    val color=if(card.suit.red) Color(0xFFB32632) else Color.Black
    Column(Modifier.width(48.dp).height(70.dp).background(Cream,RoundedCornerShape(7.dp))
        .border(if(active) 2.dp else 1.dp,if(active) Gold else Color.LightGray,RoundedCornerShape(7.dp))
        .then(if(onClick!=null) Modifier.clickable { onClick() } else Modifier).padding(4.dp),
        verticalArrangement=Arrangement.SpaceBetween) {
        Text(card.face,color=color,fontSize=13.sp,fontWeight=FontWeight.Bold)
        Text(card.suit.symbol,color=color,fontSize=22.sp,modifier=Modifier.align(Alignment.CenterHorizontally))
    }
}
