# Course API collector

Small Java/Maven app that polls several public APIs and writes the result to JSON or CSV.

## Freepik key

Freepik requires an API key. The app checks system environment first and then `.env` in the project folder.

Example `.env`:

```env
FREEPIK_API_KEY=your_key_here
```

Old name `API_KEY` is also checked as a fallback.

## Run examples

```sh
# All APIs, JSON, 10 objects per API, max 2 parallel tasks, interval 1 second
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1"
```

```sh
# Only Freepik + Emoji, JSON, 5 objects per API
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=freepik,emoji --format=json --output=icons --count=5 --threads=2 --interval=1"
```

```sh
# Only RandomUser, CSV, 15 objects
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --apis=randomuser --format=csv --output=users --count=15 --threads=1 --interval=1"
```

```sh
# All APIs, CSV, append to existing file. If CSV headers changed, old rows are migrated.
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=csv --output=data --append --count=10 --threads=3 --interval=1"
```

```sh
# Print only selected API results after saving
mvn exec:java -Dexec.mainClass="com.voronina.course.Main" -Dexec.args="--auto --format=json --output=result --count=10 --threads=2 --interval=1 --print-apis=randomuser,emoji"
```

```sh
# Interactive mode
mvn exec:java -Dexec.mainClass="com.voronina.course.Main"
```

## Tests and coverage

```sh
mvn test
```

```sh
mvn verify
```

Coverage report is generated here:

```text
target/site/jacoco/index.html
```

The Jacoco check is configured for at least 70% line coverage.
