# Walka: Minecraft + statystyki Ki Craft

Aktywny tryb używa standardowych ataków Minecrafta: LPM, odnowienia ataku zależnego od broni, krytyków, ataku obszarowego miecza i tarczy pod PPM. Zwykłe PPM nadal służy do używania przedmiotów. Ctrl + PPM pozostaje skrótem technik Ki.

Statystyki i formy Ki Craft nadal wpływają na obrażenia i odporność. Pozostają trening przez trafienia, zużycie staminy i wpływ staminy na siłę ciosów. Wyczerpanie nie dodaje własnego spowolnienia odnowienia ataku; trafienia nie nakładają własnego ogłuszenia.

Własne ciosy lewej/prawej ręki, combo, losowe mocne ciosy, garda bez tarczy pod X i parowanie nie są dostępne przez sterowanie ani pakiety gracza. Steruje tym `CombatRules.CUSTOM_MELEE_ENABLED = false`. Dawny silnik i jego testy pozostają w repozytorium do przyszłych prac; nie opisują aktywnej mechaniki gry.

## Oozaru

Saiyan lub Half-Saiyan z ogonem, w formie bazowej i bez statusu Legendary Saiyan, rozpoczyna przemianę po spojrzeniu na widoczny księżyc podczas pełni. Kierunek księżyca odpowiada obrotom nieba w Minecraft 1.20.1; tolerancja celowania wynosi 8 stopni. Przeszkody na linii wzroku oraz całkowicie deszczowe niebo blokują rozpoczęcie.

Sekwencja trwa 60 ticków serwera (3 sekundy przy 20 TPS). Serwer zapamiętuje obrót gracza, a klient blokuje obrót myszą do zakończenia. Sekwencja czyści namierzanie przeciwnika. Śmierć, wylogowanie, zmiana wymiaru, utrata ogona, zmiana na nieuprawnioną rasę/formę lub koniec pełni przerywają sekwencję. Po ukończeniu kamera zostaje odblokowana. Dach nie cofa ukończonej przemiany; koniec pełni i utrata ogona nadal ją kończą.

## Weryfikacja

Testy serwera znajdują się w `tests/gametest/java`, a konfiguracja w `tests/gametest.init.gradle`. Uruchomienie: `java -classpath gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --offline -I tests/gametest.init.gradle runGameTestServer`. Testy i arena nie trafiają do produkcyjnego JAR-a.

Nowe testy sprawdzają zwykłe ataki i tarczę, odrzucanie pakietów własnej walki, wpływ statystyk, kierunek księżyca, zasłonięcie go, czas sekwencji, utrzymanie obrotu i anulowanie przemiany. Wizualne działanie blokady myszy wymaga sprawdzenia w kliencie, również podczas lotu i w kamerze trzecioosobowej.

Protokół sieciowy: 18. Klient i serwer muszą używać zgodnej wersji moda.
