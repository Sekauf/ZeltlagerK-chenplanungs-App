# Zeltlager Küchenplanungs-App

Dieses Projekt bildet die Grundlage für eine modulare Küchenplanungs-Anwendung für Zeltlager. Die aktuelle Version stellt die Architektur bereit, sodass zukünftige Erweiterungen wie ein Menüplaner oder eine Lagerbestandsverwaltung nahtlos integriert werden können.

## Projektstruktur

Das Projekt verwendet Gradle und Java 21. Die Anwendung ist in drei Schichten gegliedert:

- **GUI** (`de.zeltlager.kuechenplaner.gui`): Präsentationsschicht. Aktuell wird eine einfache Konsolenoberfläche bereitgestellt, die die Zusammenarbeit der Schichten demonstriert. Später kann sie durch eine Desktop- oder Web-Oberfläche ersetzt werden.
- **Logik** (`de.zeltlager.kuechenplaner.logic`): Enthält die Geschäftslogik. Services kapseln Anwendungsfälle wie das Verwalten des Menüplans oder des Lagerbestands und delegieren an Repositories.
- **Daten** (`de.zeltlager.kuechenplaner.data`): Verantwortlich für Datenmodelle und Repositories. Die momentanen In-Memory-Implementierungen können später durch persistente Lösungen (z. B. Datenbanken oder externe APIs) ersetzt werden.

Die Klassen werden im Einstiegspunkt `de.zeltlager.kuechenplaner.App` zusammengeführt.

## Entwicklung

### Voraussetzungen

- Java 21
- Gradle (Wrapper liegt bei)

### Projekt ausführen

```bash
./gradlew run
```

### Tests ausführen

```bash
./gradlew test
```

### Windows-Installer erstellen

Das Projekt kann mit Hilfe von [jpackage](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html) als eigenständige Windows-Executable verpackt werden. Voraussetzung ist eine Java-21-Installation mit enthaltenem `jpackage`-Tool (z. B. das Oracle JDK oder das Temurin JDK). Führe anschließend auf einem Windows-System:

```powershell
./gradlew jpackage
```

Der Installer (`.exe`) sowie das entpackte Runtime-Image werden unter `build/jpackage/` abgelegt. Die Anwendung bringt dabei eine eigene schlanke Java-Laufzeitumgebung mit und lässt sich dadurch offline starten.

## Nächste Schritte

- Implementierung einer echten Benutzeroberfläche (Desktop oder Web)
- Persistenter Speicher für Menüpläne und Lagerbestände
- Erweiterte Geschäftslogik (z. B. automatische Einkaufslisten, Tagespläne, Rollen- und Rechteverwaltung)
