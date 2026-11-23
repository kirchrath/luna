# Luna

Luna ist eine zentrale Anwendung zur Verwaltung des Alltags, betrieben im Heimnetz.
Architektur, Tech-Stack und Konventionen stehen in [AGENTS.md](AGENTS.md).

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
