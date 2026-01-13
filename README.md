# Oppgavesett 1.2: Webapplikasjoner

## Oversikt

Oppgavesettet 1.2 består av 4 deloppgaver som introduserer webapplikasjoner, HTTP-API-er, og CRUD-operasjoner. Fokus er på å forstå separasjonen mellom frontend og backend, samt sikkerhet og ytelse i webapplikasjoner.

Nummerering "1.2" tolkes slik at den første "1" betyr første del av grunnpensum og den andre "2" betyr nummer til oppgavesettet i grunnpensum.   
   

**Estimert tid:** 8-10 timer totalt

### Oppgave 1: Enkel READ-API (ca. 1.5 timer)
- **Fokus:** HTTP GET, JSON-respons, API-design
- **Tema:** Frontend vs Backend, separasjon av bekymringer
- **Oppgave:** Utforske en webserver som tilbyr en GET-API for å hente studentdata fra CSV og returnere som JSON

### Oppgave 2: API med Søk (ca. 2 timer)
- **Fokus:** Query-parametere, input-validering, SQL injection
- **Tema:** Sikkerhet i webapplikasjoner, validering på frontend vs backend
- **Oppgave:** Utforske søkefunksjonalitet med eksempel på SQL injection-sårbarhet

### Oppgave 3: CRUD-API med UPDATE (ca. 2 timer)
- **Fokus:** HTTP PUT, datamodifikasjon, datakonsistens
- **Tema:** POST vs PUT, atomare operasjoner, concurrency
- **Oppgave:** Utforske UPDATE-operasjon for å endre studentdata

### Oppgave 4: CRUD-API med DELETE og Analytics (ca. 2.5 timer)
- **Fokus:** HTTP DELETE, kompleks dataanalyse, API-design
- **Tema:** Soft delete, statistikk, aggregering
- **Oppgave:** Utforske DELETE-operasjon og analyse-endepunkt for quiz-resultater

---

## Programmeringsmiljø og Testing

### Krav
- **Java:** OpenJDK 21+ (eller annen Java-implementasjon)
- **Nettverkstilgang:** For å teste API-er lokalt trenger man å ha konfigurert lokal nettverksgrensesnitt (som skal egentlig ikke gå ut på Internett)
- **curl:** For å teste API-er fra kommandolinje (eller Postman/lignende)

### Valg av IDE
Du kan bruke:
- IDE (IntelliJ IDEA, Eclipse, VS Code med Java-utvidelser)
- Frittstående editor (Sublime Text, VS Code) ANBEFALT
- Kommandolinje (javac + java) VIKTIG

### Testing lokalt

Denne repository inneholder skript for lokal testing. Du må sørge for at skriptene har nødvendige rettigheter.

Skriptene kan innehold feil, derfor anbefales det å teste hver oppgave separat.

**macOS og Linux:**
```bash
$ git clone <repository_med_oppgaven>
$ cd <repository_navn>
$ chmod 755 test-local.sh
$ ./test-local.sh
```

**Windows (PowerShell):**
```powershell
> Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
> .\test-local.ps1
```

**Windows (Command Prompt):**
```cmd
> test-local.bat
```

---

## Deloppgaver

### Oppgave 1: Enkel READ-API

**Mål:** Forstå HTTP GET, JSON-serialisering, og API-design

**Oppgave:**

1. Studer Java-klasse `StudentAPI.java` som implementerer en enkel webserver 

2. Finn ut hvordan man kan generere en korrekt JSON output (se linje 108 i `StudentAPI.java`) og skriv koden. Kompiler `StudentAPI.java` med `javac` fra kommandolinje. 

3. Finn ut hvilke endepunkter er implementerert og test disse. 
4. Sjekk også ut `frontend-eksempel.html` (du kan åpne den direkte i en nettleser).

**Bruk:**

*OBS! På Windows blir det brukt "slash" eller solidus U+002F og ikke "backslash" eller revers solidus U+005C.*

```bash
cd oppgave1
java StudentAPI 8000 ../testdata/data/studenter.csv
```

**Test (eksempel):**
```bash
curl http://localhost:8000/api/students
```

**Forventet output:**
```json
[
  {"id":101,"name":"Mickey","program":"CS"},
  {"id":102,"name":"Daffy","program":"EE"},
  ...
]
```

#### Refleksjonsspørsmål:
- S1: Hva er fordelene med å bruke JSON i stedet for CSV for API-respons?
- S2: Hvordan kan en frontend-applikasjon (nettleser) bruke denne API-en?
- S3: Hvordan ville du testet denne API-en manuelt (bortsett fra med `curl`, som allerede er vist i oppgaveteksten)?

---

### Oppgave 2: API med Søk og Path Traversal/SQL Injection

**Mål:** Forstå søkefunksjonalitet, input-validering, og sikkerhet

**Oppgave a):**
1. Studer Java-klasse `FileAccessAPI.java` og finn ut hvilke endepunkter den implementerer.
2. Finn ut og forstå hvordan man kan aksessere filen `secret.txt`, som ligger høyere i filsystemets hierarki enn mappen som serveren starter på.
3. Bruk Path.normalize() metoden for å beskytte mot navigering i filesystemet ved hjelp av endepunktet. Kompiler `FileAccessAPI.java` på nytt og test resultatet.

**Bruk:**

*OBS! På Windows blir det brukt "slash" eller solidus U+002F og ikke "backslash" eller revers solidus U+005C.*

```bash
cd oppgave2
java FileAccessAPI 8010 ../testdata/data/brukere.csv
```

**Test:**
```bash
curl "http://localhost:8010/api/files?filename=studenter.csv"
curl "http://localhost:8010/api/search?query=bruker2"
```
*OBS! det er lagt inn en test, som sjekker om query innelholder `' OR '1'='1`, som da gir den klassiske SQL-injection eksemplet hvor det blir passert 1=1 inn til databasehåndteringssystemet, som da tolker dette som en setning som alltid er sann.*

**Oppgave b):**

*OBS! SQL-injection er her meget naivt simulert (sjekker kun på at %27 tegnet er i parameterverdien). Vi kommer tilbake til dette sikkerhetsproblemet når vi skal bruke databasehåndteringssystemer istedenfor filer for datalagring.* 

1. Studer Java-klasse `UserSearchAPI.java` som implementerer søkefunksjonalitet
2. Finn ut hvilke endepunkter den implementerer og test disse. 
3. Sammenligne den sårbare og den sikre versjonen.


**Bruk:**

*OBS! På Windows blir det brukt "slash" eller solidus U+002F og ikke "backslash" eller revers solidus U+005C.*

```bash
cd oppgave2
java UserSearchAPI 8001 ../testdata/data/brukere.csv
```

**Test:**
```bash
curl "http://localhost:8001/api/search?email=bruker1@epost.no"
curl "http://localhost:8001/api/search-safe?email=bruker1@epost.no"
```

#### Refleksjonsspørsmål:

Del a):
- S1: Hva er Path Traversal (Directory Traversal), og hvorfor er det farlig?
- S2: Hvordan kan du beskytte koden din mot Path Traversal?

Del b):
- S1: Hva er SQL injection, og hvordan kan det oppstå i en webapplikasjon?
- S2: Hvordan kan du beskytte deg mot SQL injection?
- S3: Hva er forskjellen på validering på frontend vs backend?
- S4: Hva er likheten mellom Path Traversal og SQL Injection?


---

### Oppgave 3: CRUD-API med UPDATE

**Mål:** Forstå CRUD-operasjoner, HTTP-metoder, og datakonsistens

**Oppgave:**

1. Studer Java-klasse `StudentCRUDAPI.java` som implementerer CRUD-operasjoner
2. Finn ut hvilke endepunkter den implementerer og test disse.
3. Utfordring: implementer **2. Opprett ny student (POST)** i `frontend/frontend-eksempel.html`


**Bruk:**

*OBS! På Windows blir det brukt "slash" eller solidus U+002F og ikke "backslash" eller revers solidus U+005C.*

```bash
cd oppgave3
java StudentCRUDAPI 8002 ../testdata/data/studenter.csv
```

**Test:**
```bash
# GET
curl http://localhost:8002/api/students/101

# PUT
curl -X PUT -H "Content-Type: application/json" \
     -d '{"name":"Mickey Mouse","program":"CS"}' \
     http://localhost:8002/api/students/101

# POST
curl -X POST -H "Content-Type: application/json" \
     -d '{"name":"NewStudent","program":"EE"}' \
     http://localhost:8002/api/students
```

#### Refleksjonsspørsmål:
- S1: Hva er forskjellen mellom POST og PUT?
- S2: Hvordan sikrer du at data forblir konsistent når du oppdaterer en fil?
- S3: Hva skjer hvis to brukere prøver å oppdatere samme student samtidig?

---

### Oppgave 4: CRUD-API med DELETE og Analytics

**Mål:** Forstå DELETE-operasjoner, dataanalyse, og statistikk

**Oppgave:**

1. Studer Java-klasse `QuizAnalyticsAPI.java` som implementerer DELETE og analyse av quiz-resultater
2.  Finn ut hvilke endepunkter den implementerer og test disse.

For Quiz-resultatene beregnes:
   - Gjennomsnittlig score per quiz
   - Standardavvik
   - Min og max score
   - Antall deltakere

For Student-statistikk beregnes
   - Gjennomsnittlig prosentpoeng
   - Antall quizer tatt


*OBS! På Windows blir det brukt "slash" eller solidus U+002F og ikke "backslash" eller revers solidus U+005C.*

**Bruk:**
```bash
java QuizAnalyticsAPI 8003 ../testdata/data/studenter.csv ../testdata/data/quiz-res.csv
```

**Test:**
```bash
curl http://localhost:8003/api/analytics/quiz-stats
curl http://localhost:8003/api/analytics/student-stats/101
curl -X DELETE http://localhost:8003/api/students/101
```

#### Refleksjonsspørsmål:
- S1: Hva er implikasjonene av å tillate DELETE-operasjoner?
- S2: Hvordan ville du implementert "soft delete"?
- S3: Hvordan kan du sikre at statistikk er korrekt når data endres?

---

## Testdata

Testdata finnes i `testdata/data/`:

- `studenter.csv` - Studentdata
- `brukere.csv` - Brukerdata
- `quiz-res.csv` - Quiz-resultater

### Format

**studenter.csv:**
```
101,Mickey,CS
102,Daffy,EE
103,Donald,CS
104,Minnie,PSY
105,Goofy,EE
```

**brukere.csv:**
```
1,bruker1@epost.no,Navn Navnesen 1
2,bruker2@epost.no,Navn Navnesen 2
...
```

**quiz-res.csv:**
```
quiz_id,student_id,score,max_score
1,101,85,100
1,102,92,100
...
```

---

## Refleksjonsspørsmål

Alle refleksjonsspørsmål skal besvares i `besvarelse-refleksjon.md`. Dette dokumentet inneholder:
- Spørsmål for hver oppgave
- Plass for dine svar

---

## Innlevering

1. Gjør alle oppgavene
2. Besvare alle refleksjonsspørsmål i `besvarelse-refleksjon.md`
3. Test lokalt med `test-local.sh` (macOS/Linux) eller `test-local.ps1` (Windows) (kan være feil i skriptene)
4. Gjør `git add`, `git commit`, og `git push`
5. Sjekk GitHub Actions for autograding-resultater

---

## Ressurser

- [HTTP-spesifikasjon](https://tools.ietf.org/html/rfc7231)
- [JSON-format](https://www.json.org/)
- [REST-prinsipper](https://restfulapi.net/)
- [OWASP Top 10 - SQL Injection](https://owasp.org/www-community/attacks/SQL_Injection)
- [Java HttpServer dokumentasjon](https://docs.oracle.com/en/java/javase/21/docs/api/com.sun.net.httpserver/module-summary.html)

---

## Lisens

CC0-1.0 (Public Domain)
