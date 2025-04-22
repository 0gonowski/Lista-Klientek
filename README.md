# ListaKlientek - Aplikacja do Zarządzania Klientami

Prosta aplikacja desktopowa stworzona w JavaFX do zarządzania listą klientek, śledzenia ich zakupów oraz naliczania rabatów w ramach programu lojalnościowego.

## Funkcjonalności

* **Zarządzanie Klientami:**
    * Dodawanie nowych klientek (imię i nazwisko, opcjonalny numer telefonu, kwota pierwszego zakupu).
    * Wyświetlanie listy klientek w tabeli z możliwością sortowania.
    * Usuwanie klientek z bazy.
    * Wyszukiwanie klientek po imieniu, nazwisku lub numerze telefonu.
* **Śledzenie Zakupów:**
    * Dodawanie kolejnych zakupów dla istniejących klientek.
    * Wyświetlanie łącznej kwoty zakupów oraz kwoty ostatniego zakupu.
    * Możliwość cofnięcia *ostatniego* dodanego zakupu (pod pewnymi warunkami).
* **System Rabatowy:**
    * Naliczanie rabatu od kwoty każdego zakupu (z zaokrągleniem i minimalną wartością 1 zł).
    * **Cykl stawek rabatowych:** Rabat procentowy zmienia się cyklicznie (2% -> 5% -> 10% -> 5% -> 15% -> 20% -> 2%...) po każdym odebraniu "dużego rabatu".
    * **Duży rabat jednorazowy:** Klientka kwalifikuje się do odebrania skumulowanego rabatu, gdy:
        * Osiągnie określony próg łącznej kwoty zakupów (progi generowane dynamicznie wg zmodyfikowanego ciągu Fibonacciego, zaczynając od 1000 zł).
        * LUB dokona określonej liczby zakupów (domyślnie 5) od ostatniego odebrania rabatu.
    * Wizualne wyróżnienie klientek uprawnionych do odbioru rabatu (zielone tło wiersza).
    * Możliwość "odebrania" dużego rabatu przez użytkownika aplikacji.
    * Resetowanie stawki rabatowej do 2% w Nowy Rok.
    * Wyświetlanie szczegółowych informacji o statusie rabatowym klientki.
* **Logowanie i Bezpieczeństwo:**
    * Prosty system logowania użytkowników (dane logowania predefiniowane w kodzie).
    * Rejestrowanie aktywności użytkowników (dodawanie/usuwanie klientów, dodawanie/cofanie zakupów, odbieranie rabatów, logowania) w pliku `activity_log.txt`.
    * Automatyczne czyszczenie pliku logów co 14 dni.
    * Ograniczenie dostępu do rejestru aktywności tylko dla wybranych użytkowników (np. "Edyta").
* **Interfejs Użytkownika:**
    * Przejrzysty interfejs oparty na JavaFX.
    * Walidacja danych wejściowych (format imienia i nazwiska, format i unikalność numeru telefonu, format kwoty).
    * Ciemny motyw interfejsu (`dark-theme.css`).

## Technologie

* **Język:** Java (JDK 11 lub nowszy zalecany)
* **Framework GUI:** JavaFX
* **Styling:** CSS (dla motywu JavaFX)

## Uruchomienie Projektu

1.  **Wymagania:**
    * Zainstalowany **JDK** (Java Development Kit), wersja 11 lub nowsza.
    * Zainstalowany **JavaFX SDK** LUB skonfigurowane środowisko IDE do pracy z JavaFX (np. poprzez Maven/Gradle lub manualne dodanie bibliotek).
2.  **Klonowanie Repozytorium:**
    ```bash
    git clone [https://github.com/TWOJA_NAZWA_UŻYTKOWNIKA/NAZWA_REPOZYTORIUM.git](https://github.com/TWOJA_NAZWA_UŻYTKOWNIKA/NAZWA_REPOZYTORIUM.git)
    cd NAZWA_REPOZYTORIUM
    ```
3.  **Otwarcie w IDE:**
    * Zaimportuj projekt do swojego ulubionego środowiska IDE (np. IntelliJ IDEA, Eclipse).
    * Upewnij się, że IDE poprawnie rozpoznało projekt Java i zależności JavaFX.
4.  **Konfiguracja JavaFX (jeśli wymagane):**
    * W zależności od wersji JDK i konfiguracji IDE, może być konieczne dodanie modułów JavaFX do konfiguracji uruchomieniowej. Zazwyczaj potrzebne są moduły `javafx.controls` i `javafx.fxml`. Przykład opcji VM:
        ```
        --module-path /ścieżka/do/javafx-sdk-xx/lib --add-modules javafx.controls,javafx.fxml
        ```
        *Zastąp `/ścieżka/do/javafx-sdk-xx/lib` rzeczywistą ścieżką do bibliotek JavaFX SDK.*
5.  **Uruchomienie:**
    * Znajdź i uruchom klasę `com.example.listaklientek.ClientApp`.

## Użycie Aplikacji

1.  Po uruchomieniu pojawi się okno logowania.
2.  Użyj jednego z predefiniowanych loginów i haseł (patrz poniżej).
3.  Główne okno aplikacji wyświetla listę klientek.
4.  Użyj formularza na dole, aby dodać nowe klientki.
5.  Użyj pola wyszukiwania na górze, aby filtrować listę.
6.  W tabeli:
    * Kliknij przycisk `+` w wierszu, aby dodać nowy zakup dla danej klientki.
    * Kliknij przycisk `Odbierz`, aby zrealizować dostępny duży rabat.
    * Kliknij przycisk `...`, aby otworzyć dodatkowe opcje (cofnij zakup, usuń klientkę, info o rabacie).

## Dane Logowania

Aplikacja używa predefiniowanych danych logowania (hardcoded):

| Login  | Hasło     | Dostęp do Rejestru |
| :----- | :-------- | :---------------- |
| `x`    | `x`       | Nie               |
| `Ewa`  | `kot2312` | Nie               |
| `Monika`| `banan112`| Nie               |
| `Edyta`| `Ser2115`| **Tak** |

**Uwaga:** Jest to uproszczony mechanizm logowania na potrzeby demonstracyjne. W środowisku produkcyjnym należy zastosować bezpieczniejsze metody zarządzania użytkownikami i hasłami.

## Przechowywanie Danych

* **Dane Klientek:** Zapisywane są w pliku `clients.csv` w głównym katalogu aplikacji. Format CSV: `Imię,Telefon,ŁącznaKwota,OstatniZakup,OstatniPróg,LiczbaZakupów,AktualnaStawkaRabatu,PoprzedniFib,AktualnyFib,ZablokowanyCofanie`
* **Log Aktywności:** Zapisywany w pliku `activity_log.txt`.
* **Data Ostatniego Czyszczenia Logu:** Zapisywana w pliku `last_clean.txt`.

## Licencja

Ten projekt jest udostępniany na licencji MIT - zobacz plik [LICENSE](LICENSE) (jeśli go dodasz) po szczegóły.
