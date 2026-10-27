# Etap 1.1 — poziomy treningowe i bonusy

Pięć statystyk przechowuje poziom treningowy (początkowo 1) i osobne XP. Bonusy rasowe są dodawane podczas obliczeń, nie wpisywane do poziomów. Przykład: siła Saiyana 1,15 składa się z poziomu treningowego 1 oraz bonusu +0,15. Bonus pozostaje addytywny; etap nie zmienia go w procentowy mnożnik.

## API i obliczenia

- `trainedStat(stat)` — zapisany poziom treningowy, używany przez awanse, wymagania XP, powiadomienia i komendę ustawiania poziomu.
- `racialStatBonus(stat)` — bonus bieżącej rasy.
- `stat(stat)` — poziom z bonusem rasy, przed wzorami zależnymi od formy. Dotychczasowi odbiorcy statystyk w rozgrywce nadal używają tej wartości.
- `statBreakdown(stat)` — rozbicie dla interfejsu. Parametr formy nie oznacza mnożenia zapisanego poziomu.
- `meleeDamageMultiplier()` i `damageReduction()` — wspólne wzory efektu siły/wytrzymałości i aktywnej formy, używane przez walkę oraz ekran szczegółów. Dalsze modyfikatory (np. stamina, pancerz i cechy rasy) pozostają osobnymi etapami obrażeń.
- `weakestStats()` — niezmienny zbiór wszystkich statystyk o najniższym wytrenowanym poziomie. Remisy nie są arbitralnie rozstrzygane. Historia przydziałów i losowanie należą do przyszłych wyzwań dziennych.

Obecny balans zasobów pozostaje zachowany: stamina nadal ma zapas 100 i poziom poprawiający efektywność. Zmiana wielkości zapasu, ekonomii XP, nagród przeciwników i pełny reset postaci po zmianie rasy to kolejne podetapy.

## Zapis i zgodność

`DataVersion` wzrasta z 4 do 5. W wersji 5 pole NBT `Stats` zawiera wyłącznie wytrenowane poziomy; `StatXp` zachowuje oddzielne XP. Przy migracji wersji 4 odejmowany jest tylko addytywny bonus bieżącej rasy. Zachowujemy dotychczasową wartość statystyki w rozgrywce, XP, odblokowania oraz pozostałe dane. Dawne migracje zapisów sprzed wersji 4 wykonywane są przed rozdzieleniem bonusów.

Migracja jest jednorazowa. Kolejne zapisy, synchronizacje i odrodzenia używają wersji 5, więc nie odejmują ponownie bonusów. XP wymagane do awansu jest odtąd liczone od poziomu treningowego: przy dodatnim bonusie rasowym próg może być nieco niższy niż wcześniej, ale zgromadzone XP nie jest usuwane.

Protokół sieciowy 18 wymaga zgodnej wersji klienta i serwera. Nie należy wczytywać nowych zapisów starszym modem: wersja 4 interpretuje pole `Stats` inaczej.

## Interfejs i weryfikacja

Ekran statystyk pokazuje poziomy treningowe; gwiazdka oznacza każdą z najsłabszych statystyk. Kliknięcie otwiera sześć kart z poziomem, bonusem rasy, wartością bazową, parametrem formy, XP oraz przykładowym efektem w rozgrywce.

`StatProgressionGameTests` sprawdza wszystkie rasy, migrację i powtarzane wczytywanie, zachowanie XP, brak zapisywania bonusu formy jako treningu, remisy oraz naprawę nieprawidłowych danych. Pełny zestaw uruchamia `tests/gametest.init.gradle`. Wygląd nowego ekranu wymaga również sprawdzenia w kliencie.
