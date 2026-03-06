PlainQuery is a local-first desktop application that allows users to query CSV files using natural language. It provides an intuitive interface for data analysis without requiring knowledge of SQL.

## Features

- Query CSV files using natural language
- SQL generation from natural language queries
- Real-time results display with data visualization
- Query history and session management
- Multiple AI provider support (OpenAI, Llama 3, Groq, Gemini)
- CSV, Excel, and JSON export capabilities
- SQL syntax highlighting and formatting
- Interactive charts (bar, line, pie, scatter)
- Dark theme with modern UI design
- Cross-platform support (Windows, macOS, Linux)

## Technologies

- JavaFX 21
- SQLite
- Apache POI
- Jackson JSON
- JFreeChart
- Maven

## Installation

1. Ensure you have Java 21 installed
2. Clone the repository
3. Navigate to the project directory
4. Run `mvn clean install` to build the project
5. Run `mvn javafx:run` to start the application

## Usage

1. Launch the application
2. Load one or more CSV files
3. Enter a natural language query in the query input field
4. Click "Run Query" to generate and execute SQL
5. View results in the results panel
6. Visualize data with interactive charts
7. Save queries to history for future reference
8. Manage query sessions for better organization

## Configuration

- API keys for AI providers can be configured in the Settings panel
- SQL generation and validation settings can be customized
- Display preferences and chart settings are available

## Contributing

Contributions are welcome. Please follow the standard GitHub workflow.

## License

PlainQuery is released under the MIT License.
