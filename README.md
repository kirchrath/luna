# Luna

Luna ist eine zentrale Anwendung zur Verwaltung des Alltags, betrieben im Heimnetz.
Architektur, Tech-Stack und Konventionen stehen in [AGENTS.md](AGENTS.md).

## Infrastruktur im Docker Swarm starten

Die Infrastruktur-Services (Traefik, step-ca, PostgreSQL, Keycloak) stehen in
[`swarm/infrastructure.yml`](swarm/infrastructure.yml), Kafka in [`swarm/kafka.yml`](swarm/kafka.yml).
Weitere Stack-Dateien (Frontend, Backends) kommen später dazu; alle werden im selben Stack `luna` deployt.

| Service | Image | Ports (veröffentlicht) |
|---|---|---|
| Traefik | `traefik:v3.7.19` | 80 (Umleitung auf HTTPS), 443 |
| step-ca | `smallstep/step-ca:0.30.2` | – (intern 9000) |
| PostgreSQL | `postgres:18.7` | 7432 |
| Keycloak | `quay.io/keycloak/keycloak:26.8.2` | 7080 (unter `/auth`) |
| Kafka | `apache/kafka:4.4.0` | 7094 |

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
              kafka_admin_password kafka_luna_core_password; do
  openssl rand -base64 24 | tr -d '\n' | docker secret create "$secret" -
done
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

#### 4. Umgebungsvariablen setzen

```bash
# Aktuelle Version des Kafka-Zertifikats (beim ersten Start: 1). Pflicht – ohne sie bricht der Deploy ab.
export LUNA_KAFKA_CERT_VERSION=1

# Nur auf dem Server: Adresse, die Kafka externen Clients mitteilt (Standard: localhost:7094).
export LUNA_KAFKA_HOST=luna.internal:7094

# Nur lokal: Issuer der Tokens passend zur lokalen Keycloak-URL (Standard: https://luna.internal/auth).
export LUNA_KEYCLOAK_URL=http://localhost:7080/auth
```

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

#### 7. Root-Zertifikat holen, einchecken und verteilen (First start only)

Root-Zertifikat aus step-ca holen, prüfen und einchecken – Details im Abschnitt
[Root-Zertifikat aus step-ca holen und einchecken](#1-root-zertifikat-aus-step-ca-holen-und-einchecken).
Anschließend auf allen Geräten installieren, die Luna nutzen.

#### 8. Kafka-Zertifikat erzeugen (First start only)

```bash
swarm/kafka/create-certificate.sh "$LUNA_KAFKA_CERT_VERSION"
```

Das Skript legt beim ersten Aufruf den step-ca-Provisioner `kafka` (max. 1 Jahr) an, startet step-ca neu und
erzeugt die Secrets `kafka_keystore_v<n>` und `kafka_keystore_password_v<n>`.

#### 9. Gesamten Stack deployen

```bash
docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml luna
```

Beim ersten Start von Kafka legt das Startskript ([`swarm/kafka/start.sh`](swarm/kafka/start.sh)) den Superuser
`admin` an. Der Service `kafka-init` ([`swarm/kafka/init.sh`](swarm/kafka/init.sh)) legt bei jeder Änderung seiner
Konfiguration die Benutzer und ACLs aller Services aus `LUNA_KAFKA_SERVICES` in `swarm/kafka.yml` an.

#### 10. Prüfen

```bash
docker service ls
docker service logs luna_kafka-init
```

- Alle Services zeigen `1/1`. Ausnahme: `kafka-init` beendet sich nach getaner Arbeit und zeigt dann `0/1`.
- Das Log von `kafka-init` endet mit `Kafka users and ACLs are up to date.`

#### 11. Geräte einrichten (First start only)

- Auf jedem Gerät in der hosts-Datei: `<IP des Servers>  luna.internal`
- Root-Zertifikat aus Schritt 7 installieren.

Danach ist Keycloak unter `https://luna.internal/auth` erreichbar (Admin-Konsole: `https://luna.internal/auth/admin`).
Lokal: PostgreSQL unter `localhost:7432`, Keycloak unter `http://localhost:7080/auth`, Kafka unter
`localhost:7094` (TLS + SCRAM).

### Wartung

**Kafka-Zertifikat jährlich erneuern:** Skript mit der nächsten Version aufrufen und mit dieser Version deployen:

```bash
export LUNA_KAFKA_CERT_VERSION=2
swarm/kafka/create-certificate.sh "$LUNA_KAFKA_CERT_VERSION"
docker stack deploy -c swarm/infrastructure.yml -c swarm/kafka.yml luna
```

Alte Versionen können danach mit `docker secret rm kafka_keystore_v1 kafka_keystore_password_v1` entfernt werden.

**Neuen Service an Kafka anbinden:**

1. Secret `kafka_<service>_password` anlegen (Bindestriche als `_`, z. B. `kafka_luna_mail_password`).
2. In `swarm/kafka.yml` beim Service `kafka-init` eine Zeile in `LUNA_KAFKA_SERVICES` ergänzen
   (`<service>: <Topics, die er lesen darf>`) und das Secret unter `secrets` eintragen (Service und Datei-Ende).
3. Neu deployen (Schritt 9) – `kafka-init` läuft bei jeder Änderung seiner Konfiguration erneut.
   Ohne Änderung erneut ausführen: `docker service update --force luna_kafka-init`.

### Festlegungen der Infrastruktur

| Thema | Festlegung |
|---|---|
| Keycloak-Admin | Benutzer `admin`, Passwort aus Secret `keycloak_admin_password`. Das ist der **Bootstrap-Admin** von Keycloak: Er ist zum Anlegen eines dauerhaften Admin-Kontos gedacht und sollte danach ersetzt werden. |
| DB-Benutzer | heißen wie ihre Datenbanken: `keycloak` (Datenbank `keycloak`) und `luna` (Datenbank `luna`) |
| Secrets | alle extern, siehe oben |
| Netzwerk | Overlay-Netz `luna`, `attachable` – auch Container außerhalb des Stacks können sich verbinden |
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
- **Kafka-ACLs:** `kafka-init` ergänzt Benutzer und Rechte nur; entfernte Services oder Lese-Freigaben müssen
  von Hand mit `kafka-acls.sh --remove` entfernt werden.
- **Swarm-Load-Balancing:** Swarm nutzt für Service-IPs IPVS im Kernel. Fehlt es (z. B. in manchen Sandboxes
  oder Containern), schlagen Verbindungen mit `no route to host` fehl. Auf einem normalen Linux-Server ist IPVS
  vorhanden.

## Root-Zertifikat von step-ca in die Backend-Images bringen

Im Stack stellt **step-ca** die Zertifikate für `luna.internal` aus. Die Backends rufen Keycloak über
Traefik per HTTPS auf (`https://luna.internal/auth`) und müssen dem Root-Zertifikat von step-ca deshalb vertrauen.

Vorgehen:

1. Das öffentliche Root-Zertifikat liegt im Repo unter `certs/luna-root-ca.crt`.
2. Beim Maven-Build erzeugt jedes Backend einen eigenen **Truststore**: die Standard-CAs des JDK
   plus das Root-Zertifikat von step-ca.
3. **Jib** legt den Truststore unter `/opt/luna/truststore.p12` ins Image.
4. Die JVM wird per `jvmFlags` angewiesen, diesen Truststore zu verwenden.

> Der **private Schlüssel** der CA verlässt step-ca nie und kommt niemals ins Repo.
> Nur das öffentliche Root-Zertifikat (`root_ca.crt`) wird eingecheckt.

### 1. Root-Zertifikat aus step-ca holen und einchecken

Einmalig nach der Initialisierung von step-ca (und erneut, falls die CA neu aufgesetzt wird).
Im offiziellen Image `smallstep/step-ca` liegt das Root-Zertifikat unter `/home/step/certs/root_ca.crt`:

```bash
mkdir -p certs
docker exec "$(docker ps -q -f name=step-ca)" cat /home/step/certs/root_ca.crt > certs/luna-root-ca.crt
```

Fingerprint prüfen und mit der Ausgabe von step-ca beim Initialisieren vergleichen:

```bash
openssl x509 -in certs/luna-root-ca.crt -noout -subject -enddate -fingerprint -sha256
```

Danach einchecken:

```bash
git add certs/luna-root-ca.crt
git commit -m "chore: add step-ca root certificate"
```

### 2. Truststore beim Build erzeugen (`pom.xml` des Backends)

Das `maven-antrun-plugin` kopiert die `cacerts` des JDK, mit dem Maven läuft (Java 25), und importiert
das Root-Zertifikat per `keytool`. Ergebnis: `target/jib-truststore/truststore.p12`.

Der Pfad `${project.basedir}/../certs/` funktioniert, weil jedes Backend ein Top-Level-Ordner im Repo ist.

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-antrun-plugin</artifactId>
  <executions>
    <execution>
      <id>create-truststore</id>
      <phase>prepare-package</phase>
      <goals>
        <goal>run</goal>
      </goals>
      <configuration>
        <target>
          <mkdir dir="${project.build.directory}/jib-truststore"/>
          <copy file="${java.home}/lib/security/cacerts"
                tofile="${project.build.directory}/jib-truststore/truststore.p12"
                overwrite="true"/>
          <exec executable="${java.home}/bin/keytool" failonerror="true">
            <arg line="-importcert -noprompt -alias luna-root-ca"/>
            <arg line="-file ${project.basedir}/../certs/luna-root-ca.crt"/>
            <arg line="-keystore ${project.build.directory}/jib-truststore/truststore.p12"/>
            <arg line="-storetype PKCS12 -storepass changeit"/>
          </exec>
        </target>
      </configuration>
    </execution>
  </executions>
</plugin>
```

> `changeit` ist das Standardpasswort der JDK-`cacerts`. Der Truststore enthält nur öffentliche Zertifikate;
> das Passwort schützt nur vor Veränderung und ist kein Geheimnis.

### 3. Truststore per Jib ins Image legen und aktivieren

```xml
<plugin>
  <groupId>com.google.cloud.tools</groupId>
  <artifactId>jib-maven-plugin</artifactId>
  <configuration>
    <from>
      <image>amazoncorretto:25</image>
      <platforms>
        <platform>
          <architecture>amd64</architecture>
          <os>linux</os>
        </platform>
      </platforms>
    </from>
    <extraDirectories>
      <paths>
        <path>
          <from>${project.build.directory}/jib-truststore</from>
          <into>/opt/luna</into>
        </path>
      </paths>
    </extraDirectories>
    <container>
      <jvmFlags>
        <jvmFlag>-Djavax.net.ssl.trustStore=/opt/luna/truststore.p12</jvmFlag>
        <jvmFlag>-Djavax.net.ssl.trustStoreType=PKCS12</jvmFlag>
        <jvmFlag>-Djavax.net.ssl.trustStorePassword=changeit</jvmFlag>
      </jvmFlags>
    </container>
  </configuration>
</plugin>
```

### 4. Image bauen

Der Truststore entsteht in der Phase `prepare-package`. Deshalb immer zusammen mit `package` bauen:

```bash
cd luna-core
./mvnw package jib:dockerBuild
```

### 5. Prüfen

Ist das Root-Zertifikat im Image-Truststore?

```bash
docker run --rm --entrypoint sh luna-core -c \
  '"$JAVA_HOME"/bin/keytool -list -keystore /opt/luna/truststore.p12 -storepass changeit -alias luna-root-ca'
```

Im laufenden Stack: Die Backend-Logs dürfen beim Abruf von `https://luna.internal/auth/...` keine
`PKIX path building failed`-Fehler zeigen.

### Hinweise

- **Lokale Entwicklung** braucht den Truststore nicht: Lokal rufen die Backends Keycloak direkt unter
  `http://localhost:7080/auth` auf.
- **Öffentliche CAs** stammen aus dem JDK, mit dem gebaut wird, nicht aus dem Basis-Image.
  Backends regelmäßig neu bauen, damit die CA-Liste aktuell bleibt.
- **Neue CA:** Wird step-ca neu initialisiert, ändert sich das Root-Zertifikat. Dann Schritt 1 wiederholen
  und **alle** Backends neu bauen und deployen. Außerdem das neue Root-Zertifikat auf allen Geräten installieren.
- **Neue Backends** übernehmen die Konfiguration aus Schritt 2 und 3 unverändert.
