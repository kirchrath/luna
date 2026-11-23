# Luna

Luna ist eine zentrale Anwendung zur Verwaltung des Alltags, betrieben im Heimnetz.
Architektur, Tech-Stack und Konventionen stehen in [AGENTS.md](AGENTS.md).

## Infrastruktur im Docker Swarm starten

Die Infrastruktur-Services (Traefik, step-ca, PostgreSQL, Keycloak) stehen in
[`swarm/infrastructure.yml`](swarm/infrastructure.yml). Weitere Stack-Dateien (Frontend, Backends)
kommen später dazu und werden im selben Stack `luna` deployt.

| Service | Image | Ports (veröffentlicht) |
|---|---|---|
| Traefik | `traefik:v3.7.19` | 80 (Umleitung auf HTTPS), 443 |
| step-ca | `smallstep/step-ca:0.30.2` | – (intern 9000) |
| PostgreSQL | `postgres:18.7` | 7432 |
| Keycloak | `quay.io/keycloak/keycloak:26.8.2` | 7080 (unter `/auth`) |

### 1. Swarm initialisieren (einmalig)

```bash
docker swarm init
```

### 2. Secrets anlegen (einmalig)

Alle Secrets sind `external` und werden direkt auf dem Server angelegt – nie im Repo.
Beispiel mit zufälligen Passwörtern:

```bash
for secret in step_ca_password postgres_password keycloak_db_password luna_db_password keycloak_admin_password; do
  openssl rand -base64 24 | tr -d '\n' | docker secret create "$secret" -
done
```

Das Keycloak-Admin-Passwort vorher notieren bzw. selbst wählen, z. B.:

```bash
printf '%s' 'mein-passwort' | docker secret create keycloak_admin_password -
```

| Secret | Verwendung |
|---|---|
| `step_ca_password` | Passwort der CA-Schlüssel von step-ca |
| `postgres_password` | Superuser `postgres` |
| `keycloak_db_password` | DB-Benutzer `keycloak` (Datenbank `keycloak`) |
| `luna_db_password` | DB-Benutzer `luna` (Datenbank `luna`) |
| `keycloak_admin_password` | Keycloak-Bootstrap-Admin `admin` |

### 3. Stack deployen

```bash
docker stack deploy -c swarm/infrastructure.yml luna
```

Beim **ersten** Start:

- step-ca erzeugt seine CA (Root-Zertifikat, ACME-Provisioner `acme`).
- PostgreSQL legt per Init-Skript ([`swarm/postgres/create-databases.sh`](swarm/postgres/create-databases.sh))
  die Datenbanken `keycloak` und `luna` an. Das Skript läuft nur bei leerem Datenverzeichnis.
- Traefik kann das Zertifikat für `luna.internal` erst beziehen, wenn step-ca bereit (healthy) ist.
  Danach Traefik einmal neu starten:

  ```bash
  docker service update --force luna_traefik
  ```

Anschließend das Root-Zertifikat holen und einchecken (siehe nächster Abschnitt, Schritt 1) und auf allen
Geräten installieren, die Luna nutzen.

### 4. Erreichbarkeit

- Auf jedem Gerät in der hosts-Datei: `<IP des Servers>  luna.internal`
- Keycloak: `https://luna.internal/auth` (Admin-Konsole: `https://luna.internal/auth/admin`)

### Lokale Entwicklung

Dieselbe Datei kann lokal genutzt werden (`docker swarm init` und Secrets wie oben). PostgreSQL ist dann
unter `localhost:7432`, Keycloak unter `http://localhost:7080/auth` erreichbar.
Damit der Issuer der Tokens zur lokalen URL passt, beim Deploy die Keycloak-URL setzen:

```bash
LUNA_KEYCLOAK_URL=http://localhost:7080/auth docker stack deploy -c swarm/infrastructure.yml luna
```

### Festlegungen der Infrastruktur

| Thema | Festlegung |
|---|---|
| Keycloak-Admin | Benutzer `admin`, Passwort aus Secret `keycloak_admin_password`. Das ist der **Bootstrap-Admin** von Keycloak: Er ist zum Anlegen eines dauerhaften Admin-Kontos gedacht und sollte danach ersetzt werden. |
| DB-Benutzer | heißen wie ihre Datenbanken: `keycloak` (Datenbank `keycloak`) und `luna` (Datenbank `luna`) |
| Secrets | `step_ca_password`, `postgres_password`, `keycloak_db_password`, `luna_db_password`, `keycloak_admin_password` (alle extern, siehe oben) |
| Netzwerk | Overlay-Netz `luna`, `attachable` – auch Container außerhalb des Stacks können sich verbinden |
| Keycloak-URL | Variable `LUNA_KEYCLOAK_URL` setzt den Issuer der Tokens. Standard: `https://luna.internal/auth`; lokal `http://localhost:7080/auth` |
| Ports | Traefik 80/443, PostgreSQL 7432, Keycloak 7080 – auch bei lokaler Nutzung belegt Traefik 80 und 443 |

### Bekannte Fallstricke

- **Erster Start:** Traefik startet vor step-ca und bekommt dann noch kein Zertifikat.
  Nach dem ersten Start, sobald step-ca healthy ist: `docker service update --force luna_traefik`.
- **Healthcheck von step-ca:** Der eingebaute Healthcheck des Images ruft `https://step-ca:9000` auf. Swarm trägt
  einen Service aber erst nach bestandenem Healthcheck ins DNS ein, dadurch wurde step-ca ständig neu gestartet.
  Die Stack-Datei überschreibt den Healthcheck daher mit `https://localhost:9000`.
- **Zeitzone:** Alle Container binden `/etc/localtime` vom Host ein. Getestet mit Europe/Berlin: Traefik, step-ca,
  PostgreSQL, Keycloak und Java in `amazoncorretto:25` liefern den richtigen Offset (z. B. `+01:00`).
  Java meldet als Zonen-ID allerdings einen gleichwertigen Alias (z. B. `Atlantic/Jan_Mayen` statt `Europe/Berlin`),
  weil die eingebundene Datei keinen Zonennamen enthält. Zeiten und Sommerzeit sind trotzdem korrekt.
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
