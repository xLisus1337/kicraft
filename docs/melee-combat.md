# Ciężkie ciosy, parowanie i garda

Pasek technik włącza się przez Ctrl + scroll. Ciosy używają przypisanego w Minecraft klawisza ataku (domyślnie LPM), bez nowego skrótu. Ładowanie działa także na pasku przedmiotów, gdy zaczynasz atak w przeciwnika lub w powietrze. Rozpoczęcie kopania bloku na pasku przedmiotów zachowuje zwykłe zachowanie Minecrafta.

Wciśnięcie ataku rozpoczyna oczekiwanie na rozpoznanie kliknięcia albo przytrzymania. Przy puszczeniu przycisku klient natychmiast rozpoczyna animację ręki, bez oczekiwania na drogę pakietu do serwera i z powrotem; serwer w tym samym żądaniu rozstrzyga cel, obrażenia i koszt. Szybkie kliknięcie nie pokazuje paska. Po 5 tickach przytrzymania pod celownikiem pojawia się pasek z procentem mocy i podpowiedzią puszczenia przycisku. Po puszczeniu lub anulowaniu pasek znika. Cios rozpoczęty w przeciwnika nie zamienia się w kopanie po przesunięciu celownika na blok.

- Krótkie kliknięcie: lekki cios przy puszczeniu przycisku.
- Cztery trafione lekkie ciosy w ten sam cel tworzą combo. Kolejne etapy mają mnożniki 1,00 / 1,08 / 1,16 / 1,35, a czwarty cios kończy serię mocnym odrzutem. Na następne trafienie jest 16 ticków (0,8 s) od poprzedniego. Pudło, blok, parowanie, zmiana celu, otrzymanie obrażeń, ciężki atak lub przerwanie wejścia zerują serię.
- Przytrzymanie od 5 ticków (0,25 s): rozpoczyna ładowanie ciężkiego ciosu i pokazuje pasek. Pełna moc po 24 tickach (1,2 s). Po osiągnięciu pełnej mocy cios nadal czeka na puszczenie LPM.
- Pełny cios: mnożnik obrażeń 2,4, koszt staminy 4 razy większy od lekkiego ciosu, nacisk na gardę 3,5 razy większy. Nadal obowiązują statystyki, formy i osłabienie od niskiej staminy.
- Koszt jest pobierany raz za próbę, również po pudle lub sparowaniu. XP wymaga trafienia. Ciężki cios wymaga wystarczającej staminy; wyświetla komunikat przy jej braku. Rasy z nieskończoną energią zachowują zwolnienie z kosztów.
- Blok (domyślnie X) chroni od przodu. Pierwsze 4 ticki dają możliwość sparowania jednego bezpośredniego ciosu wręcz. Puszczanie i wciskanie bloku nie omija 12-tickowej przerwy między oknami parowania. Zwykłe trzymanie bloku nadal działa.
- Parowanie zatrzymuje obrażenia, kosztuje niewiele staminy i daje 16 ticków (0,8 s) na jedną kontrę przeciw temu samemu przeciwnikowi z bonusem 35%. Złoty X oraz komunikat sygnalizują sukces. Atakujący jest odsłonięty przez 10 ticków.
- Pełny ciężki cios przebija zwykłą gardę także rasom z nieskończoną energią. Słabsze ciosy przełamują ją po wyczerpaniu staminy. Przełamanie trwa 20 ticków, a blok zatrzymuje tylko 20% przełamującego uderzenia.
- Nietrafiony lub zatrzymany ciężki cios odsłania na 16 ticków. W odsłonięciu nie można atakować, blokować ani wykonać uniku i otrzymuje się o 25% więcej obrażeń od ataków żywych przeciwników.
- Trafiony ciężki cios ma 10 ticków przerwy, lekki 6. Dodatkowo nadal działa czas odnowienia ataku Minecrafta.
- Menu, przełączenie paska, utrata fokusu, zmiana świata, zmiana trzymanego przedmiotu i trafienie podczas ładowania przerywają cios. Trzymanie ponad 5 sekund wygasza próbę. Nowa próba wymaga ponownego naciśnięcia.

Podczas ładowania i powrotu do gotowości nie można równocześnie strzelać Ki, ładować Ki, przekazywać Ki ani wykonywać uniku. Sam ruch pozostaje dostępny.

Serwer mierzy czas od BEGIN do RELEASE i sam sprawdza cel przez promień od oczu, zasięg oraz przeszkody. Klient nie podaje obrażeń, czasu ładowania ani identyfikatora celu. Protokół sieciowy: 7; klient i serwer muszą używać tej samej wersji moda.

## Sprawdzanie

Testy logiki czasu: `.codex-build/melee-tests/MeleeTimelineCheck.java`. Testy w odizolowanym serwerze Minecraft: `.codex-build/melee-gametest-src`, uruchamiane przez `.codex-build/melee-gametest.init.gradle`. Testy i arena nie są dodawane do zwykłego JAR-a.

Weryfikacja 2026-09-09: 129 sprawdzeń reguł czasu, 74 sprawdzenia obsługi LPM, 16 sprawdzeń combo oraz 22 testy GameTest zakończyły się powodzeniem. GameTest sprawdza między innymi pełną serię 1–4, wzrost obrażeń, odrzut finishera i reset combo, granicę lekkiego i ciężkiego ciosu na 4/5 tickach, powtórzenie RELEASE, przełamanie gardy pełnym ciosem, parowanie z kontrą, koszt pudła bez XP, przeszkodę pomiędzy graczami, brak staminy oraz anulowanie i przerwanie ładowania trafieniem. Gracze testowi mają wyłączoną tylko w teście ochronę po pojawieniu się; w modzie pozostaje ona bez zmian.

W kliencie należy sprawdzić klikanie/przytrzymywanie LPM (również po zmianie przypisania), przełączanie pasków podczas ładowania, otwieranie menu, widoczność HUD w pierwszej osobie i kamerze zza ramienia oraz F1. PvP warto sprawdzić z drugim klientem przy rzeczywistym opóźnieniu sieci.
