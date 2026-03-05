# PlainQuery

A local-first desktop application for querying CSV files using natural language.

## Requirements

- Java 17 or later
- Maven 3.8 or later

## Build

```
mvn clean package
```

## Run

```
mvn javafx:run
```

Or with the fat JAR (after package):

```
java -jar target/plainquery-fat.jar
```

## Configuration

On first launch, open Settings to configure:

- AI provider (Groq or Gemini)
- API key for the selected provider
- Database mode (in-memory or file-based)
- Maximum preview rows (default 10,000)

## Privacy

Only the natural language question and table/column names with sample values are
sent to the configured AI API. Row data never leaves the local machine.

## Architecture

```
controller  ->  service interfaces  ->  service implementations
                                              |
                                         db / util
```

No circular dependencies. All service wiring is done manually in App.java.

## License

MIT
