# luna-core

Erstes Backend von Luna (Port 7081, Produktion unter `/api/luna-core`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-core` ist die **Event-Analyse** von Luna. Kurz gesagt:

> **Events kommen rein, werden analysiert und erzeugen neue Events.**

- `luna-core` **empfängt Events aus unterschiedlichen Quellen** (andere Services).
- Jedes Event wird **analysiert**. Die Analyse wird mit **Spring AI** umgesetzt.
- Aus der Analyse entscheidet `luna-core`, **welche neuen Elemente bzw. Events** entstehen.

### Beispiele

| Eingehendes Event | Ergebnis der Analyse |
|---|---|
| Ein Service liefert ein Event über eine E-Mail, deren Inhalt etwas zu erledigen verlangt | neue **Aufgabe** |
| Die E-Mail enthält einen wichtigen Hinweis zu einem Systemstatus | neuer **Alert** |
| Die E-Mail ist ein Unzustellbarkeitsbericht (Non-Delivery-Report) | neuer **Alert** |

## Funktionsliste

> [!NOTE]
> Die Liste ist ein erster Entwurf und **muss noch kuratiert werden**. Kein Punkt ist zur Umsetzung freigegeben.

| # | Funktion | Status |
|---|---|---|
| 1 | Events aus unterschiedlichen Quellen empfangen | _TODO_ |
| 2 | Eingehende Events per Spring AI analysieren | _TODO_ |
| 3 | Anhand der Analyse entscheiden, welche neuen Elemente entstehen (z. B. Aufgabe, Alert) | _TODO_ |
| 4 | Neue Events für die entstandenen Elemente erzeugen | _TODO_ |

## Entscheidungen

- **Eingang:** Events kommen über **Kafka** oder per **manueller Eingabe im Frontend**
  (das Frontend ruft `luna-core` wie alle Backends über den generierten REST-Wrapper auf).
  Weitere Message Broker erst in einer **späteren Ausbaustufe** (nicht umsetzen, solange nicht beauftragt).
- **Erste Quelle:** **manuelle Eingabe** im Frontend.
- **Ausgang:** Entstandene Elemente (z. B. Aufgabe, Alert) werden als Events **an die zuständigen Fach-Backends
  gesendet**, die sie speichern. `luna-core` speichert diese Elemente nicht selbst.
- **Ausgangskanal:** `luna-core` publiziert die erzeugten Events auf **Kafka-Topics**; die Fach-Backends
  konsumieren sie.
- **Event-Format:** **eigenes JSON-Format** (ein- und ausgehend), definiert als Records. Die genaue Struktur
  (Felder, Metadaten) ist noch offen.
- **Spring AI:** Modell bzw. Anbieter ist **konfigurierbar** (per Property/Profil umschaltbar).
  Zunächst unterstützt: **Ollama** (lokal), **Anthropic Claude** und **OpenAI-kompatible** Endpunkte.
- **Kafka-Betrieb:** Image **`apache/kafka`** (offizielles Apache-Image, KRaft-Modus, ein Knoten) im Stack.

## Offene Punkte

Noch nicht entschieden (bitte hier eintragen, sobald entschieden):

- **Kafka-Version und -Konfiguration:** exakte Image-Version und Einbindung in den Stack. _TODO_
- **Event-Struktur:** Felder und Metadaten des eigenen JSON-Formats. _TODO_
- **Topics:** Benennung und Aufteilung der Kafka-Topics. _TODO_
- **Element-Typen:** Welche Ergebnistypen gibt es außer Aufgabe und Alert? _TODO_
- **Spring-AI-Standard:** Welcher Anbieter ist Standard je Profil (`local`, `prod`)? _TODO_
- **Persistenz:** Was speichert `luna-core` selbst (z. B. eingegangene Events, Analyseergebnisse)? _TODO_
- **Abgrenzung:** Was gehört ausdrücklich **nicht** zu `luna-core`? _TODO_
