package com.example.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import com.example.spacirkovnik.Answer
import com.example.spacirkovnik.ComponentType.ONE_BUTTON
import com.example.spacirkovnik.ComponentType.QUESTION
import com.example.spacirkovnik.DataHolder

class DataRepository {

    private val h1 = DataHolder("Lesnícka palica Gejzu Dražďáka", "Štart", fontSize = 26.sp, componentType = ONE_BUTTON)
    private val h2 = DataHolder(
        "Kde bolo, tam bolo, žil raz v petržalských lesoch jeden lesník. " +
        "Tento lesník sa volal Gejza Dražďák a mal na starosti lesy na petržalskej strane Dunaja. " +
        "Pomocou svojej lesníckej palice dokázal udržiavať poriadok v lese. ")
    private val h3 = DataHolder(
        "Táto palica bola totiž kúzelná. Pomáhala mu rozprávať sa so zvieratami a tak hneď vedel, " +
        "či je v lese všetko v poriadku.")
    private val h4 = DataHolder(
        "Táto lesnícka palica mu dokonca dala moc rozumieť zvieratám aj keď ju práve nemal pri sebe." +
        " Stačilo, že ju ráno chytil do ruky a hneď rozumel všetkým zvieratkám celý deň.")
    private val h5 = DataHolder(
        "Raz sa stalo, že sa mu táto lesnícka palica stratila. " +
        "Gejza netušil kde sa jeho lesnícka palica nachádza. " +
        "Bez toho aby vedel kde je jeho kúzelná palica sa nemohol vybrať do lesa.")
    private val h6 = DataHolder(
        "Chcel by si pomôcť Gejzovi nájsť stratenú lesnícku palicu?",
        "Áno",
        componentType = ONE_BUTTON)
    private val h7 = DataHolder("Poďme na to:")
    private val h8 = DataHolder("Ako prvu vec potrebujem aby si zistil kedy bol postaveny tento dom. Bolo to:",
                                componentType = QUESTION,
                                answers = listOf(
                                    Answer("1981", false),
                                    Answer("1982", false),
                                    Answer("1983", true),
                                    Answer("1984", false)))
    private val h9 = DataHolder("Správne! Teraz musíš ísť za ježkom možno ti poradí ako ďalej")
    private val h10 = DataHolder(
        "Ahoj pomocník Ja som ježko a bývam tu na tejto lúke. Pomáhaš nájsť lesnícku palicu?",
        "Áno",
        componentType = ONE_BUTTON)
    private val h11 = DataHolder("Tak poď za mnou. Dúfam že mi budeš stíhať, ja som totiž celkom rýchly.")
    private val h12 = DataHolder("Povedz mi kde bol vyrobený tento maličký most, lebo sa mi veľmi páči",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Podbrezová", false),
                                     Answer("Hronec", true),
                                     Answer("Košice", false),
                                     Answer("Bratislava", false)))
    private val h13 = DataHolder("Ďakujem za informáciu. " +
                                 "Videl som, že niekto v kapucni beží tam na konci tejto lúky s lesníkovou palicou v ruke. " +
                                 "Tam neďaleko býva korytnačka, skús sa jej spýtať či niekoho nevidela.")
    private val h14 = DataHolder("Ahoj pomocník, vidím že pomáhaš nájsť lesnícku palicu. " +
                                 "Môžem ti pomôcť, ale najprv musíš dokázať že dobre poznáš zvieratká. ")
    private val h15 = DataHolder("Vieš medzi aké druhy zvierat patria korytnačky?",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Cicavce", false),
                                     Answer("Obojživejníky", true),
                                     Answer("Ryby", false),
                                     Answer("Vtáky", false)))
    private val h16 = DataHolder("Výborne, vidím že sa vyznáš. Takže ti pomôžem." +
                                 "Videla som niekoho v kapucni. Išiel tadiaľto po ceste. Tam kúsok dalej na poloostrove býva bobor. " +
                                 "Skús sa spýtať jeho",
                                 "Ďakujem",
                                 componentType = ONE_BUTTON)
    private val h17 = DataHolder("Čo tu chceš, nevidíš že nemám čas? " +
                                 "Jasné, jasné, hľadáš palicu, a prečo ti ja mám pomáhať? ")
    private val h18 = DataHolder("Vieš vôbec čo majú bobry najradšej?",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Listy zo stromov", false),
                                     Answer("Kôru zo stromov", true),
                                     Answer("Suchú trávu", false),
                                     Answer("Čerstvé mušky", false)))
    private val h19 = DataHolder("No tak dobre, pomôžem ti. " +
                                 "Videl som niekoho v kapucni utekať týmto smerom. Tam niekde býva vlk, skús sa spýtať jeho. ")
    private val h20 = DataHolder("Auuuuuuuu! " +
                                 "Čo tu robíš? Vari nevidíš že si na mojom území? " +
                                 "Ty sa ma nebojíš? Tak to musíš byť ty, čo hľadáš tú lesníkovu palicu? " +
                                 "Už o tebe vie hádam celý les. A tuším chceš pomoc aj odo mňa. " +
                                 "Videl som niekoho, kto mal v ruke tú palicu. " +
                                 "Ale ak chceš moju pomoc, musíš mi odpovedať na jednu otázku: ")
    private val h21 = DataHolder("V ktorej rozprávke nie je vlk v hlavnej úlohe?",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Sedem kozliatok", false),
                                     Answer("Tri prasiatka", false),
                                     Answer("Červená čiapočka", false),
                                     Answer("Šípková Ruženka", true)))
    private val h22 = DataHolder("Správne. Pozri, už je tu veverička. Tá ti ukáže kadiaľ išiel ten človek s palicou. ")
    private val h23 =
            DataHolder("Áno, áno, ja ti to ukážem, ale najprv mi nusíš vysvetliť ako tu mohol vyrásť taký veľý strom. " +
                       "Poď za mnou ukážem ti ho.")
    private val h24 = DataHolder("Ach veverička, veď to nie je strom, to je predsa:",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Veľký dom", false),
                                     Answer("Panelák", false),
                                     Answer("Krík", false),
                                     Answer("Stožiar", true)))
    private val h25 = DataHolder("A na tom stožiari sú antény na príjmanie a vysielanie signálu.")
    private val h26 = DataHolder("Antény? No teda. No keď vravíš. " +
                                 "No teraz musíš ísť tamto k vode. Skús sa spýtať labute, či ti nebude vedieť poradiť viac.")
    private val h27 = DataHolder("Ahoj labuť. Nevidela si tu niekoho s lesníkovou palicou?")
    private val h28 = DataHolder("Videla, ale ak chces pomôcť, tak najprv poraď ty mne.")
    private val h29 = DataHolder("Vieš ktorá rozprávka je o labuti?",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Škaredé mačiatko", false),
                                     Answer("Škaredé kuriatko", false),
                                     Answer("Škaredé húsatko", false),
                                     Answer("Škaredé káčatko", true)))
    private val h30 =
            DataHolder("No dobre, videla som aj ja niekoho ísť s lesníckou palicou, musíš ísť tamto kúsok ďalej. " +
                       "Tam sa zvykne nad hladinu vynárať kapor, počkaj na neho, on určite niečo videl.")
    private val h31 = DataHolder("Počkaj na kapra? Ale dokedy tu mám čakať?")
    private val h32 = DataHolder("Hmm, niekto ma tu volá? Aha, hľadáš palicu, že? " +
                                 "Pomôžem ti ak vieš, ktorá časť kapra sa odkladá pre šťastie.")
    private val h33 = DataHolder("Je to:",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Kaprie oko", false),
                                     Answer("Kapria kosť", false),
                                     Answer("Kapria hlava", false),
                                     Answer("Kapria šupina", true)))
    private val h34 = DataHolder("No dobre, vidím že to poznáš, tak choď týmto smerom. " +
                                 "Tam pri lávke býva žaba, hádam ti aj ona pomôže.")
    private val h35 = DataHolder("Kváá. Kváá. Vidím ťa. A viem že hľadáš lesníkovú palicu. " +
                                 "Ale ak chceš vedieť kadiaľ ďalej, musíš vedieť ako sa volá táto časť Dunaja!")
    private val h36 = DataHolder("Je to:",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("Slovenské rameno", false),
                                     Answer("Maďarské rameno", false),
                                     Answer("Poľské rameno", false),
                                     Answer("Chorvátske rameno", true)))
    private val h37 = DataHolder("Výborne! Kváá. " +
                                 "Teraz bež ešte tamto do lesa. Tam musíš nájsť strom v tvare písmena \"V\". " +
                                 "Na ňom býva stará múdra sova, ona už bude vedieť ako ti poradiť.")
    private val h38 = DataHolder("Hu húú, tak ty hľadáš palicu? Tak mám pre teba poslednú úlohu. " +
                                 "Povedz mi kam išla sova v obľúbenej detskej pesničke?",
                                 componentType = QUESTION,
                                 answers = listOf(
                                     Answer("do kina", false),
                                     Answer("do divadla", false),
                                     Answer("na tanec", true),
                                     Answer("na disco", false)))
    private val h39 = DataHolder("Super. Zvládol si všetky úlohy. Teraz už musíš len zistiť kde je lesníková palica. " +
                                 "Ale ani to pre teba nebude problém. Stačí ak nazrieš do Záhrady Gejzu Dražďáka.")
    private val h40 = DataHolder("V záhrade: Veď tá palica je tu. A má ju v rukách lesníkov syn. " +
                                 "Tak tá palica sa vôbec nestratila, ale lesníkov syn spravil všetku prácu za svojho otca! ")
    private val _myTexts = mutableStateOf(listOf(h1,
                                                 h2,
                                                 h3,
                                                 h4,
                                                 h5,
                                                 h6,
                                                 h7,
                                                 h8,
                                                 h9,
                                                 h10,
                                                 h11,
                                                 h12,
                                                 h13,
                                                 h14,
                                                 h15,
                                                 h16,
                                                 h16,
                                                 h17,
                                                 h18,
                                                 h19,
                                                 h20,
                                                 h21,
                                                 h22,
                                                 h23,
                                                 h24,
                                                 h25,
                                                 h26,
                                                 h27,
                                                 h28,
                                                 h29,
                                                 h30,
                                                 h31,
                                                 h32,
                                                 h33,
                                                 h34,
                                                 h35,
                                                 h36,
                                                 h37,
                                                 h38,
                                                 h39,
                                                 h40))
}