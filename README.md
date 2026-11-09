# Pearl Trail (Fabric 1.21.4)

Smuga z fioletowych cząsteczek teleportacji endermana za rzuconą perłą kresu – od rzutu do miejsca uderzenia.
Blok, w który uderzy perła, dostaje pulsującą ramkę i unoszące się cząsteczki.
Mod tylko po stronie klienta (działa też na serwerach bez moda).

## Ustawienia
W grze wciśnij **O** (klawisz można zmienić w Opcje → Sterowanie → Pearl Trail).
Można ustawić: włączenie smugi, gęstość, czas smugi po uderzeniu, wielkość wybuchu,
podświetlenie bloku, czas podświetlenia i jego kolor. Ustawienia zapisują się w `config/pearltrail.json`.

## Jak zbudować przez GitHub
1. Wrzuć całą zawartość folderu (razem z `.github`) do repozytorium na GitHubie.
2. Zakładka **Actions** – build odpali się sam (albo *Build → Run workflow*).
3. Po kilku minutach pobierz `pearl-trail` z sekcji **Artifacts** zakończonego buildu.
4. Wrzuć `pearl-trail-1.1.0.jar` (bez `-sources`) do `.minecraft/mods` razem z Fabric API.
