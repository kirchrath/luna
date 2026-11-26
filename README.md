# Luna

Luna ist eine zentrale Anwendung zur Verwaltung des Alltags, betrieben im Heimnetz.
Architektur, Tech-Stack und Konventionen stehen in [AGENTS.md](AGENTS.md).

## Infrastruktur im Docker Swarm starten

Die Infrastruktur-Services (Traefik, step-ca, PostgreSQL, Keycloak) stehen in
[`swarm/infrastructure.yml`](swarm/infrastructure.yml), Kafka in [`swarm/kafka.yml`](swarm/kafka.yml),
`luna-core` in [`swarm/luna-core.yml`](swarm/luna-core.yml), `luna-task` in [`swarm/luna-task.yml`](swarm/luna-task.yml)
und das Frontend in
[`swarm/frontend.yml`](swarm/frontend.yml). Alle werden im selben Stack `luna` deployt.

| Service | Image | Ports (veröffentlicht) |
|---|---|---|
| Traefik | `traefik:v3.7.19` | 80 (Umleitung auf HTTPS), 443 |
| step-ca | `smallstep/step-ca:0.30.2` | – (intern 9000) |
| PostgreSQL | `postgres:18.7` | 7432 |
| Keycloak | `quay.io/keycloak/keycloak:26.8.2` | 7080 (unter `/auth`) |
| Kafka | `apache/kafka:4.4.0` | 7094 |
| `luna-core` | `luna-core:${LUNA_CORE_VERSION}` (lokal gebaut) | – (über Traefik unter `/api/luna-core`) |
| `luna-task` | `luna-task:${LUNA_TASK_VERSION}` (lokal gebaut) | – (über Traefik unter `/api/luna-task`) |
| Frontend | `luna-frontend:${LUNA_FRONTEND_VERSION}` (lokal gebaut, nginx) | – (über Traefik unter `/`) |

### Schritt-für-Schritt: Infrastruktur starten

Die Anleitung gilt für den **ersten** und jeden **weiteren** Start (z. B. nach `git pull`, geänderten Stack-Dateien
oder `docker stack rm`). Schritte mit **(First start only)** sind nur beim ersten Start nötig; bei jedem weiteren
Start werden sie übersprungen. Alle Befehle laufen im Wurzelverzeichnis des Repos auf dem Server
(bzw. lokal, siehe Schritt 4).

Nach einem Neustart des Servers ist nichts zu tun: Swarm startet alle Services selbst wieder.

#### 1. Repository aktualisieren

```bash
git pull
```

#### 2. Swarm initialisieren (First start only)

```bash
docker swarm init
```

#### 3. Secrets anlegen (First start only)

Alle Secrets sind `external` und werden direkt auf dem Server angelegt – nie im Repo.
Beispiel mit zufälligen Passwörtern:

```bash
for secret in step_ca_password postgres_password keycloak_db_password luna_db_password keycloak_admin_password \
              kafka_admin_password kafka_luna_core_password kafka_luna_task_password; do
  openssl rand -base64 24 | tr -d '\n' | docker secret create "$secret" -
done
```

Den API-Key des KI-Anbieters für `luna-core` selbst eintragen (bei Ollama einen beliebigen Platzhalter):

```bash
printf '%s' '<api-key>' | docker secret create luna_core_ai_api_key -
```

Das Keycloak-Admin-Passwort vorher notieren bzw. selbst wählen, z. B.:

```bash
printf '%s' 'mein-passwort' | docker secret create keycloak_admin_password -
```

| Secret | Verwendung |
|---|---|
| `step_ca_password` | Passwort der CA-Schlüssel von step-ca und des Provisioners `kafka` |
| `postgres_password` | Superuser `postgres` |
| `keycloak_db_password` | DB-Benutzer `keycloak` (Datenbank `keycloak`) |
| `luna_db_password` | DB-Benutzer `luna` (Datenbank `luna`) |
| `keycloak_admin_password` | Keycloak-Bootstrap-Admin `admin` |
| `kafka_admin_password` | Kafka-Superuser `admin` (wird nur beim allerersten Start von Kafka übernommen) |
| `kafka_<service>_password` | Kafka-Benutzer je Service, z. B. `kafka_luna_core_password` für `luna-core` |
| `kafka_keystore_v<n>`, `kafka_keystore_password_v<n>` | TLS-Zertifikat von Kafka in Version `<n>` – erzeugt das Skript in Schritt 8 |
| `luna_core_ai_api_key` | API-Key des KI-Anbieters für `luna-core` (bei Ollama ein Platzhalter) |

#### 4. Umgebungsvariablen setzen

```bash
# Aktuelle Version des Kafka-Zertifikats (beim ersten Start: 1). Pflicht – ohne sie bricht der Deploy ab.
export LUNA_KAFKA_CERT_VERSION=1

# Nur auf dem Server: Adresse, die Kafka externen Clients mitteilt (Standard: localhost:7094).
export LUNA_KAFKA_HOST=luna.internal:7094

# Nur lokal: Issuer der Tokens passend zur lokalen Keycloak-URL (Standard: https://luna.internal/auth).
export LUNA_KEYCLOAK_URL=http://localhost:7080/auth

# luna-core: Version des lokal gebauten Images (siehe luna-core/pom.xml). Pflicht.
export LUNA_CORE_VERSION=0.0.1-SNAPSHOT

# luna-task: Version des lokal gebauten Images (siehe luna-task/pom.xml). Pflicht.
export LUNA_TASK_VERSION=0.0.1-SNAPSHOT

# Frontend: Version des lokal gebauten Images (siehe frontend/package.json). Pflicht.
export LUNA_FRONTEND_VERSION=0.0.1

# luna-core: KI-Anbieter (ollama, anthropic oder openai) und Property, unter der das Secret
# luna_core_ai_api_key eingebunden wird. Beides Pflicht.
export SPRING_AI_MODEL_CHAT=anthropic
export LUNA_AI_API_KEY_PROPERTY=spring.ai.anthropic.api-key

# luna-core: optionale Einstellungen des Anbieters, z. B.:
export SPRING_AI_ANTHROPIC_CHAT_MODEL=<modell>
# export SPRING_AI_OLLAMA_BASE_URL=http://<ollama-host>:11434
# export SPRING_AI_OLLAMA_CHAT_MODEL=<modell>
# export SPRING_AI_OPENAI_BASE_URL=<url>
# export SPRING_AI_OPENAI_CHAT_MODEL=<modell>
```

Nicht gesetzte optionale Variablen werden nicht an den Container übergeben.

Die aktuell verwendete Zertifikat-Version zeigt `docker secret ls --filter name=kafka_keystore_v`
(höchste Version verwenden).

#### 5. Infrastruktur ohne Kafka deployen (First start only)

Kafka braucht ein Zertifikat von step-ca, das erst erzeugt werden kann, wenn step-ca läuft.
Deshalb beim ersten Start zunächst nur die Infrastruktur:

```bash
docker stack deploy -c swarm/infrastructure.yml luna
```

Dabei erzeugt step-ca seine CA (Root-Zertifikat, ACME-Provisioner `acme`), und PostgreSQL legt per Init-Skript
([`swarm/postgres/create-databases.sh`](swarm/postgres/create-databases.sh)) die Datenbanken `keycloak` und `luna` an.

#### 6. Auf step-ca warten und Traefik neu starten (First start only)

Traefik startet vor step-ca und bekommt dann noch kein Zertifikat für `luna.internal`.
Warten, bis step-ca `healthy` ist, dann Traefik neu starten:

```bash
until docker ps --filter name=luna_step-ca --filter health=healthy --quiet | grep -q .; do sleep 5; done
docker service update --force luna_traefik
```

#### 7. Root-Zertifikat holen und verteilen (First start only)

Root-Zertifikat aus step-ca holen und prüfen – Details im Abschnitt
[Root-Zertifikat von step-ca](#root-zertifikat-von-step-ca).
Anschließend auf allen Geräten installieren, die Luna nutzen.

#### 8. Kafka-Zertifikat erzeugen (First start only)

```bash
swarm/kafka/create-certificate.sh "$LUNA_KAFKA_CERT_VERSION"
```

Das Skript legt beim ersten Aufruf den step-ca-Provisioner `kafka` (max. 1 Jahr) an, startet step-ca neu und
erzeugt die Secrets `kafka_keystore_v<n>` und `kafka_keystore_password_v<n>`.

#### 9. Images der Backends und des Frontends bauen

Immer, wenn sich `luna-core` geändert hat (Java 25 nötig):

```bash
(cd luna-core && ./mvnw package jib:dockerBuild)
```

Ebenso für `luna-task`, wenn es sich geändert hat:

```bash
(cd luna-task && ./mvnw package jib:dockerBuild)
```

Immer, wenn sich das Frontend geändert hat (nur Docker nötig, Node läuft im Build-Container):

```bash
docker build --platform linux/amd64 -t "luna-frontend:$LUNA_FRONTEND_VERSION" frontend
```

#### 10. Gesamten Stack deployen

```bash
docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml -c swarm/luna-core.yml -c swarm/luna-task.yml \
  -c swarm/frontend.yml luna
```

Beim ersten Start von Kafka legt das Startskript ([`swarm/kafka/start.sh`](swarm/kafka/start.sh)) den Superuser
`admin` an. Der Service `kafka-init` ([`swarm/kafka/init.sh`](swarm/kafka/init.sh)) legt bei jeder Änderung seiner
Konfiguration die Benutzer und ACLs aller Services aus `LUNA_KAFKA_SERVICES` in `swarm/kafka.yml` an.

#### 11. Prüfen

```bash
docker service ls
docker service logs luna_kafka-init
docker service logs luna_luna-core
docker service logs luna_luna-task
docker service ps luna_frontend
```

- Alle Services zeigen `1/1`. Ausnahme: `kafka-init` beendet sich nach getaner Arbeit und zeigt dann `0/1`.
- Das Log von `kafka-init` endet mit `Kafka users and ACLs are up to date.`
- Das Log von `luna-core` enthält `Started Application`; `https://luna.internal/api/luna-core/actuator/health`
  meldet `UP`. Ebenso `luna-task` unter `https://luna.internal/api/luna-task/actuator/health`.
- `https://luna.internal/` leitet zur Anmeldung bei Keycloak weiter und zeigt danach Luna.

#### 12. Geräte einrichten (First start only)

- Auf jedem Gerät in der hosts-Datei: `<IP des Servers>  luna.internal`
- Root-Zertifikat aus Schritt 7 installieren.

#### 13. Luna-Konto anlegen (First start only)

Keycloak legt beim ersten Start den Realm `luna` mit dem Client `luna-frontend` aus
[`swarm/keycloak/luna-realm.json`](swarm/keycloak/luna-realm.json) an. Benutzer enthält der Import bewusst nicht.

1. Admin-Konsole öffnen (`https://luna.internal/auth/admin`, lokal `http://localhost:7080/auth/admin`) und mit dem
   Bootstrap-Admin `admin` anmelden.
2. Oben links den Realm **luna** auswählen.
3. **Users → Create new user:** Benutzername, E-Mail und **Vorname** eintragen (der Vorname erscheint im
   Benutzermenü von Luna), **Email verified** einschalten, **Create**.
4. Reiter **Credentials → Set password:** Passwort setzen, **Temporary** ausschalten, speichern.

Danach meldet sich Luna mit diesem Konto an.

Danach ist Keycloak unter `https://luna.internal/auth` erreichbar (Admin-Konsole: `https://luna.internal/auth/admin`).
Lokal: PostgreSQL unter `localhost:7432`, Keycloak unter `http://localhost:7080/auth`, Kafka unter
`localhost:7094` (TLS + SCRAM).

### Wartung

**Kafka-Zertifikat jährlich erneuern:** Skript mit der nächsten Version aufrufen und mit dieser Version deployen:

```bash
export LUNA_KAFKA_CERT_VERSION=2
swarm/kafka/create-certificate.sh "$LUNA_KAFKA_CERT_VERSION"
docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml -c swarm/luna-core.yml -c swarm/luna-task.yml \
  -c swarm/frontend.yml luna
```

Alte Versionen können danach mit `docker secret rm kafka_keystore_v1 kafka_keystore_password_v1` entfernt werden.

**Bestehende Installation um `luna-task` erweitern:** einmalig das Secret anlegen, `LUNA_TASK_VERSION` setzen
(Schritt 4), das Image bauen (Schritt 9) und neu deployen (Schritt 10):

```bash
openssl rand -base64 24 | tr -d '\n' | docker secret create kafka_luna_task_password -
```

**Neuen Service an Kafka anbinden:**

1. Secret `kafka_<service>_password` anlegen (Bindestriche als `_`, z. B. `kafka_luna_mail_password`).
2. In `swarm/kafka.yml` beim Service `kafka-init` eine Zeile in `LUNA_KAFKA_SERVICES` ergänzen
   (`<service>: <Topics, die er lesen darf>`) und das Secret unter `secrets` eintragen (Service und Datei-Ende).
   Zu den lesbaren Topics gehören auch das eigene Retry-Topic und DLT (z. B. `luna_task_task_retry`,
   `luna_task_dlt`); `kafka-init` legt alle lesbaren Topics an, falls sie noch fehlen.
3. Neu deployen (Schritt 10) – `kafka-init` läuft bei jeder Änderung seiner Konfiguration erneut.
   Ohne Änderung erneut ausführen: `docker service update --force luna_kafka-init`.

### Festlegungen der Infrastruktur

| Thema | Festlegung |
|---|---|
| Keycloak-Admin | Benutzer `admin`, Passwort aus Secret `keycloak_admin_password`. Das ist der **Bootstrap-Admin** von Keycloak: Er ist zum Anlegen eines dauerhaften Admin-Kontos gedacht und sollte danach ersetzt werden. |
| DB-Benutzer | heißen wie ihre Datenbanken: `keycloak` (Datenbank `keycloak`) und `luna` (Datenbank `luna`) |
| Secrets | alle extern, siehe oben |
| Netzwerk | Overlay-Netz `luna`, `attachable` – auch Container außerhalb des Stacks können sich verbinden |
| Keycloak-Realm | `luna` aus `swarm/keycloak/luna-realm.json`: Anmeldeseite Deutsch (Standard) und Englisch, „Angemeldet bleiben“, keine Registrierung. Client `luna-frontend`: öffentlich, Authorization Code mit PKCE (S256), Weiterleitungen nur an `https://luna.internal/*` und `http://localhost:5173/*`. Keine Benutzer im Import (siehe Schritt 13) |
| Keycloak-URL | Variable `LUNA_KEYCLOAK_URL` setzt den Issuer der Tokens. Standard: `https://luna.internal/auth`; lokal `http://localhost:7080/auth` |
| Kafka-Adresse | Backends im Stack nutzen `kafka:9094`. Für Clients außerhalb des Stacks (Port 7094) teilt Kafka die Adresse aus `LUNA_KAFKA_HOST` mit. Standard: `localhost:7094`; auf dem Server z. B. `LUNA_KAFKA_HOST=luna.internal:7094` setzen, damit Entwicklungsrechner zugreifen können |
| Kafka | ein Knoten im KRaft-Modus (ohne ZooKeeper), feste `CLUSTER_ID` in der Stack-Datei, Daten im Volume `kafka-data` |
| Kafka-Sicherheit | TLS und SASL/SCRAM-SHA-512 auf `kafka:9094` und Port 7094; der Controller-Listener lauscht nur auf `localhost` im Container (ohne TLS). Zertifikat von step-ca (SANs `kafka`, `localhost`, `luna.internal`, 1 Jahr) über den Provisioner `kafka`, dessen Schlüssel mit `step_ca_password` geschützt ist |
| Kafka-Rechte | Je Service ein Benutzer (= Service-Name) mit ACLs: schreiben und Topics anlegen nur unter `luna_<service ohne luna->_`, lesen nur ausdrücklich freigegebene Topics, Consumer-Gruppe = Service-Name. Topics entstehen automatisch beim ersten Schreiben |
| Ports | Traefik 80/443, PostgreSQL 7432, Keycloak 7080, Kafka 7094 – auch bei lokaler Nutzung belegt Traefik 80 und 443 |

### Bekannte Fallstricke

- **Erster Start:** Traefik startet vor step-ca und bekommt dann noch kein Zertifikat – deshalb Schritt 6.
- **Healthcheck von step-ca:** Der eingebaute Healthcheck des Images ruft `https://step-ca:9000` auf. Swarm trägt
  einen Service aber erst nach bestandenem Healthcheck ins DNS ein, dadurch wurde step-ca ständig neu gestartet.
  Die Stack-Datei überschreibt den Healthcheck daher mit `https://localhost:9000`.
- **Zeitzone:** Alle Container binden `/etc/localtime` vom Host ein. Getestet mit Europe/Berlin: Traefik, step-ca,
  PostgreSQL, Keycloak und Java in `amazoncorretto:25` liefern den richtigen Offset (z. B. `+01:00`).
  Java meldet als Zonen-ID allerdings einen gleichwertigen Alias (z. B. `Atlantic/Jan_Mayen` statt `Europe/Berlin`),
  weil die eingebundene Datei keinen Zonennamen enthält. Zeiten und Sommerzeit sind trotzdem korrekt;
  das ist bewusst so akzeptiert (kein zusätzliches `TZ`).
- **Kafka-Admin-Passwort:** Wird nur beim allerersten Start (leeres Volume `kafka-data`) übernommen. Spätere
  Änderungen am Secret `kafka_admin_password` wirken nicht automatisch.
- **Pflichtvariablen in Stack-Dateien:** Die Meldung in `${VAR:?Meldung}` darf keinen Bindestrich enthalten. Docker
  liest ihn sonst als Standardwert-Operator und setzt z. B. `luna-core:core image version` statt der Version ein.
  Prüfen lässt sich das mit `docker stack config -c … ` (zeigt die eingesetzten Werte).
- **Realm-Import:** Keycloak importiert `luna-realm.json` nur, wenn der Realm `luna` noch nicht existiert. Spätere
  Änderungen an der Datei wirken nicht auf eine bestehende Installation; sie müssen zusätzlich in der Admin-Konsole
  nachgezogen werden. Außerdem lehnt Swarm geänderte Configs ab (`only updates to Labels are allowed`): Dann in
  `swarm/infrastructure.yml` den Config-Namen ändern (z. B. `keycloak_luna_realm_v2`).
- **Kafka-ACLs:** `kafka-init` ergänzt Benutzer und Rechte nur; entfernte Services oder Lese-Freigaben müssen
  von Hand mit `kafka-acls.sh --remove` entfernt werden.
- **Swarm-Load-Balancing:** Swarm nutzt für Service-IPs IPVS im Kernel. Fehlt es (z. B. in manchen Sandboxes
  oder Containern), schlagen Verbindungen mit `no route to host` fehl. Auf einem normalen Linux-Server ist IPVS
  vorhanden.

## Root-Zertifikat von step-ca

Im Stack stellt **step-ca** die Zertifikate für `luna.internal` und für Kafka aus.

- **Geräte** müssen dem Root-Zertifikat vertrauen (Browser, lokale Entwicklung).
- **Backends** lesen es **zur Laufzeit** aus dem step-ca-Volume (`/step/certs/root_ca.crt`, read-only
  eingebunden) und nutzen es als PEM-Truststore für Kafka. Keycloak erreichen sie im Stack intern per HTTP
  (`jwk-set-uri`), dafür ist kein Truststore nötig. Ein eingechecktes Zertifikat oder ein Truststore im Image ist
  daher nicht nötig.

> Der **private Schlüssel** der CA verlässt step-ca nie.

Root-Zertifikat holen (einmalig nach der Initialisierung von step-ca und erneut, falls die CA neu aufgesetzt wird):

```bash
docker exec "$(docker ps -q -f name=luna_step-ca)" cat /home/step/certs/root_ca.crt > luna-root-ca.crt
```

Fingerprint prüfen und mit der Ausgabe von step-ca beim Initialisieren vergleichen:

```bash
openssl x509 -in luna-root-ca.crt -noout -subject -enddate -fingerprint -sha256
```

Anschließend auf allen Geräten installieren. Für die lokale Entwicklung von `luna-core` wird die Datei außerdem als
Kafka-Truststore genutzt (siehe [luna-core/README.md](luna-core/README.md)).

- **Neue CA:** Wird step-ca neu initialisiert, ändert sich das Root-Zertifikat. Dann das Kafka-Zertifikat neu
  erzeugen (Wartung), alle Services neu starten und das neue Root-Zertifikat auf allen Geräten installieren.
