# luna-mail

Quelle **E-Mail** (Port 7086, Produktion unter `/api/luna-mail`).
Konventionen, Tech-Stack und Betrieb stehen in [AGENTS.md](../AGENTS.md).

## Zweck

`luna-mail` holt neue E-Mails der konfigurierten Postfächer per **IMAP** ab und gibt sie an `luna-core` weiter:

```
IMAP-Postfächer ──► Mailbox (Abrufstand) ──► Email ──► luna_mail_email (Kafka) ──► luna-core (Analyse)
```

1. Der `MailScheduler` fragt alle Konten im Abstand `app.mail.poll-interval` (Standard 1 Minute) ab, immer per TLS
   (`imaps`) und nur lesend; das Postfach wird nicht verändert (keine „gelesen“-Markierung).
2. Je Konto merkt sich eine `Mailbox` UIDVALIDITY und die höchste bekannte UID. Der **erste Abruf** eines Kontos
   merkt sich nur den aktuellen Stand: **ältere E-Mails werden nie nachgeholt**. Ändert sich die UIDVALIDITY, beginnt
   der Abruf ebenso neu ab dem aktuellen Stand (mit Log-Warnung).
3. Jede neue E-Mail wird als `Email` gespeichert und **nach dem Commit** als `EmailEventDto` auf `luna_mail_email`
   veröffentlicht (Key = ID der E-Mail).
4. Fehler (z. B. falsches Passwort, Server nicht erreichbar) stehen am Postfach: `status = FAILED`, Anzahl `failures`
   und `lastError`; der nächste erfolgreiche Abruf setzt `OK`.

Keine eigenen REST-Endpunkte und kein Push; die E-Mails erscheinen über `luna-core` in „Letzte Eingaben“.

## E-Mail

| Feld | Inhalt |
|---|---|
| `account`, `address` | Name des Kontos in der Konfiguration und seine Adresse |
| `messageId`, `from`, `to`, `subject`, `sentDate` | Kopf der E-Mail (`from`/`to` als „Name <adresse>“; `to` mit allen Empfängern) |
| `receivedDate` | Eingang im Postfach (IMAP-INTERNALDATE) |
| `text` | Text der E-Mail: der erste `text/plain`-Teil, sonst der erste `text/html`-Teil, mit **jsoup** in Text umgewandelt; gekürzt auf `app.mail.max-text-length` (Standard 10.000), dann `textTruncated = true` |
| `attachments` | Name, Typ und Größe der Anhänge, **ohne Inhalt** |

Veröffentlichte Events: [kafka-api.yaml](kafka-api.yaml) (Kopie für `luna-core` unter
`../luna-core/src/main/resources/kafka-api/luna-mail.yaml`).

## Konten

Konten stehen unter `app.mail.accounts.<name>` mit `username` (Adresse), `password` (App-Passwort), `area`
(Lebensbereich aller E-Mails des Kontos: `PERSONAL` oder `WORK`, **Pflicht** – ohne startet `luna-mail` nicht) und
optional `host`, `port` und `folder`; ohne diese gelten `app.mail.defaults` (`imap.gmail.com`, `993`, `INBOX`). So kommen auch
andere Anbieter mit IMAP ohne Codeänderung dazu.

```yaml
app:
  mail:
    accounts:
      privat:
        username: vorname.nachname@gmail.com
        password: abcd efgh ijkl mnop
        area: PERSONAL
      verein:
        username: kasse@verein.example
        password: <app-passwort>
        area: WORK
        host: imap.example.net
```

- **Produktion:** Die Datei ist das Docker Secret `mail_accounts_v<n>` (Auswahl über `LUNA_MAIL_ACCOUNTS_VERSION`),
  eingebunden als `/run/secrets/mail-accounts.yml` und per `spring.config.import` geladen. Ein neues oder geändertes
  Konto: neue Version anlegen, Variable erhöhen, neu deployen; alte Version danach mit `docker secret rm` entfernen.

  ```bash
  docker secret create mail_accounts_v2 mail-accounts.yml && rm mail-accounts.yml
  export LUNA_MAIL_ACCOUNTS_VERSION=2
  ```

- **Lokal:** `luna-mail/mail-accounts.yml` (steht in `.gitignore`, nie einchecken).

### Gmail: App-Passwort einrichten

1. Im Google-Konto die **Bestätigung in zwei Schritten** einschalten.
2. Unter <https://myaccount.google.com/apppasswords> ein App-Passwort für „Luna“ erzeugen und als `password`
   eintragen. IMAP ist bei privaten Gmail-Konten immer aktiv.
3. Bei Google-Workspace-Konten muss der Admin App-Passwörter erlauben; mit dem Advanced-Protection-Programm gibt es
   keine App-Passwörter.

Outlook.com bzw. Microsoft 365 erlauben kein IMAP mit Passwort mehr (nur OAuth mit App-Registrierung) und sind daher
noch nicht angebunden (siehe AGENTS.md, Schritt 8).

## Betrieb

- **Stack:** [`swarm/luna-mail.yml`](../swarm/luna-mail.yml), Image `luna-mail:${LUNA_MAIL_VERSION}`.
- **Secrets:** `luna_db_password` (`spring.datasource.password`), `kafka_luna_mail_password` (`app.kafka.password`),
  `mail_accounts_v<n>` (Konten).
- **Kafka:** Benutzer `luna-mail`; schreibt `luna_mail_*`, liest nichts.
- **Netz:** braucht ausgehende Verbindungen zu den IMAP-Servern (Port 993).

## Entwicklung

```bash
cd luna-mail
./mvnw verify               # bauen, testen (GreenMail per Testcontainers), Coverage prüfen
./mvnw verify -Popenapi     # openapi.yaml und kafka-api.yaml (samt Kopie für luna-core) neu erzeugen
./mvnw package jib:dockerBuild
```
